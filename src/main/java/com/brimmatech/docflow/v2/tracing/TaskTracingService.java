package com.brimmatech.docflow.v2.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.Callable;


@Service
public class TaskTracingService {

    private final Tracer tracer;
    private final RouteTracingService routeTracingService;

    public TaskTracingService(@Qualifier("defaultTracer") Tracer tracer,
                              RouteTracingService routeTracingService
                              ) {
        this.tracer = tracer;
        this.routeTracingService = routeTracingService;
    }

    public <T> T executeWithTracing(String taskName,
                                    Map<String, String> traceHeaders, Map<String, String> customAttributes,
                                    Callable<T> task) throws Exception {

        Context parentContext = traceHeaders != null
                ? routeTracingService.restoreContext(traceHeaders)
                : Context.current();

        Span span = tracer.spanBuilder(taskName)
                .setParent(parentContext)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {

            if (customAttributes != null) {
                customAttributes.forEach(span::setAttribute);
            }

            return task.call();
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

}
