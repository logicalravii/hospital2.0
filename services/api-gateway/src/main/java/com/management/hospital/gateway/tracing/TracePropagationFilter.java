package com.management.hospital.gateway.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Forwards the gateway request's trace context to the downstream service and
 * exposes the trace ID to the caller.
 *
 * <p>Spring WebFlux already observes each incoming request (continuing an incoming
 * W3C {@code traceparent} if present, otherwise starting a new trace), but the
 * Spring Cloud Gateway version in use does not hook that observation into its
 * proxied call. This filter injects the request span's context into the outgoing
 * headers, replacing any client-supplied trace headers, so the downstream service
 * joins the same trace. It also returns the trace ID as {@value #TRACE_ID_HEADER}
 * so a client can quote it when reporting a problem.
 */
@Component
public class TracePropagationFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** Before authentication, so rejected requests also carry the trace ID header. */
    static final int ORDER = -200;

    private final Propagator propagator;

    public TracePropagationFilter(Propagator propagator) {
        this.propagator = propagator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Optional<TraceContext> traceContext = currentRequestTraceContext(exchange);
        if (traceContext.isEmpty()) {
            return chain.filter(exchange);
        }
        TraceContext context = traceContext.get();

        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, context.traceId());

        ServerHttpRequest forwarded = exchange.getRequest().mutate()
            .headers(headers -> {
                propagator.fields().forEach(headers::remove);
                propagator.inject(context, headers, HttpHeaders::set);
            })
            .build();

        return chain.filter(exchange.mutate().request(forwarded).build());
    }

    private Optional<TraceContext> currentRequestTraceContext(ServerWebExchange exchange) {
        return ServerRequestObservationContext.findCurrent(exchange.getAttributes())
            .map(observationContext -> observationContext.<TracingContext>get(TracingContext.class))
            .map(TracingContext::getSpan)
            .map(Span::context);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
