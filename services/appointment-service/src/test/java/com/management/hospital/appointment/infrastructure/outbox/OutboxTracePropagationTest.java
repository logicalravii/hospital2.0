package com.management.hospital.appointment.infrastructure.outbox;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxTracePropagationTest {

    private SdkTracerProvider tracerProvider;
    private Tracer tracer;
    private OutboxTracePropagation tracePropagation;

    @BeforeEach
    void setUp() {
        tracerProvider = SdkTracerProvider.builder().build();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        io.opentelemetry.api.trace.Tracer otelTracer = sdk.getTracer("outbox-test");
        tracer = new OtelTracer(otelTracer, new OtelCurrentTraceContext(), event -> { });
        tracePropagation = new OutboxTracePropagation(tracer, new OtelPropagator(sdk.getPropagators(), otelTracer));
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    @DisplayName("Captures no traceparent when no trace is active")
    void captureReturnsEmptyWithoutActiveTrace() {
        assertThat(tracePropagation.captureCurrentTraceParent()).isEmpty();
    }

    @Test
    @DisplayName("Captures the active span as a W3C traceparent")
    void captureReturnsActiveSpanAsTraceParent() {
        Span requestSpan = tracer.nextSpan().name("http post /api/v1/appointments").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(requestSpan)) {
            String traceParent = tracePropagation.captureCurrentTraceParent().orElseThrow();

            assertThat(traceParent)
                    .hasSize(55)
                    .startsWith("00-" + requestSpan.context().traceId() + "-" + requestSpan.context().spanId());
        } finally {
            requestSpan.end();
        }
    }

    @Test
    @DisplayName("Dispatch runs in a child span of the stored trace, even with no trace on the current thread")
    void dispatchJoinsStoredTrace() {
        String storedTraceParent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        AtomicReference<Span> spanDuringDispatch = new AtomicReference<>();

        tracePropagation.runInStoredTrace(storedTraceParent, () -> spanDuringDispatch.set(tracer.currentSpan()));

        Span dispatchSpan = spanDuringDispatch.get();
        assertThat(dispatchSpan).isNotNull();
        assertThat(dispatchSpan.context().traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(dispatchSpan.context().parentId()).isEqualTo("00f067aa0ba902b7");
        assertThat(tracer.currentSpan()).as("dispatch span must not leak onto the thread").isNull();
    }

    @Test
    @DisplayName("Dispatch without a stored traceparent runs the action without inventing a trace")
    void dispatchWithoutStoredTraceRunsActionDirectly() {
        AtomicReference<Span> spanDuringDispatch = new AtomicReference<>();

        tracePropagation.runInStoredTrace(null, () -> spanDuringDispatch.set(tracer.currentSpan()));

        assertThat(spanDuringDispatch.get()).isNull();
    }

    @Test
    @DisplayName("Failure during dispatch propagates and leaves no span in scope")
    void dispatchFailurePropagates() {
        String storedTraceParent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

        assertThatThrownBy(() -> tracePropagation.runInStoredTrace(storedTraceParent, () -> {
            throw new IllegalStateException("serializer failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(tracer.currentSpan()).isNull();
    }
}
