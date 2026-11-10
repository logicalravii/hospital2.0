package com.management.hospital.appointment.infrastructure.outbox;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Carries trace context across the outbox's asynchronous gap.
 *
 * <p>An outbox event is written inside the request's transaction but published to
 * Kafka later, either right after commit or by the background poller on another
 * thread. Without help, that publish would start a new, unrelated trace. This
 * component captures the current W3C {@code traceparent} when the event is stored
 * and re-enters that trace when the event is dispatched, so the Kafka record
 * (and every consumer downstream) shares the originating request's trace ID.
 */
@Component
public class OutboxTracePropagation {

    static final String TRACEPARENT = "traceparent";
    static final String DISPATCH_SPAN_NAME = "outbox dispatch";

    private final Tracer tracer;
    private final Propagator propagator;

    public OutboxTracePropagation(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    /** Returns the current span's {@code traceparent}, if a trace is active. */
    public Optional<String> captureCurrentTraceParent() {
        Span current = tracer.currentSpan();
        if (current == null) {
            return Optional.empty();
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(current.context(), carrier, Map::put);
        return Optional.ofNullable(carrier.get(TRACEPARENT));
    }

    /**
     * Runs {@code action} inside a child span of the stored trace. If no trace was
     * captured, the action runs as-is (and starts its own trace if instrumented).
     */
    public void runInStoredTrace(String traceParent, Runnable action) {
        if (traceParent == null || traceParent.isBlank()) {
            action.run();
            return;
        }
        Span span = propagator.extract(Map.of(TRACEPARENT, traceParent), Map::get)
                .name(DISPATCH_SPAN_NAME)
                .start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            action.run();
        } catch (RuntimeException ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
        }
    }
}
