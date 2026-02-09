package com.brimmatech.docflow.v2.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class RouteTracingService {

    private final Tracer tracer;
    private final Map<String, Span> activeParentSpans = new ConcurrentHashMap<>();

    public RouteTracingService(@Qualifier("defaultTracer") Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Starts a parent span for a route and returns trace headers to propagate downstream.
     *
     * @param qualifierId unique identifier for the route/job
     * @param routeName   route name
     * @param tenantId
     * @return headers map containing W3C trace context
     */
    public Map<String, String> startParentSpan(String qualifierId, String routeName, Long tenantId) {
        Span parentSpan = tracer.spanBuilder("route:" + routeName)
                .setAttribute("qualifierId", qualifierId)
                .setAttribute("tenantId", tenantId )
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        activeParentSpans.put(qualifierId, parentSpan);

        try (Scope scope = parentSpan.makeCurrent()) {
            return ContextPropagationUtil.currentTraceHeaders();
        }
    }

    /**
     * Ends the parent span for the given qualifier ID.
     *
     * @param qualifierId unique identifier for the route/job
     */
    public void endParentSpan(String qualifierId) {
        Span span = activeParentSpans.remove(qualifierId);
        if (span != null) {
            span.end();
        }
    }

    /**
     * Get the current parent span for a qualifier.
     */
    public Span getParentSpan(String qualifierId) {
        return activeParentSpans.get(qualifierId);
    }

    /**
     * Restore context from trace headers for downstream tasks or async jobs.
     */
    public Context restoreContext(Map<String, String> headers) {
        return ContextPropagationUtil.extractContext(headers);
    }

    public Span getChildSpanOrNew(Context parentContext, String spanName) {
        return tracer.spanBuilder(spanName)
                .setParent(parentContext)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
    }

    public Span startChildSpan(String spanName, Map<String, String> parentTraceHeaders) {
        Context parentContext = this.restoreContext(parentTraceHeaders);
        return this.getChildSpanOrNew(parentContext, spanName);
    }

}
