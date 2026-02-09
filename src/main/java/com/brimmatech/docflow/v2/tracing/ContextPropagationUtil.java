package com.brimmatech.docflow.v2.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.HashMap;
import java.util.Map;

public class ContextPropagationUtil {

    private static final W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();

    // Extract context from incoming headers
    public static Context extractContext(Map<String, String> headers) {
        return propagator.extract(Context.current(), headers, new TextMapGetter<Map<String, String>>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }

            @Override
            public String get(Map<String, String> carrier, String key) {
                if (carrier.containsKey(key)) {
                    return carrier.get(key);
                }
                return null;
            }
        });
    }

    // Inject current span context into headers
    public static void injectContext(Context context, Map<String, String> headers) {
        propagator.inject(context, headers, new TextMapSetter<Map<String, String>>() {
            @Override
            public void set(Map<String, String> carrier, String key, String value) {
                carrier.put(key, value);
            }
        });
    }

    // Convenience: get current span headers
    public static Map<String, String> currentTraceHeaders() {
        Map<String, String> headers = new HashMap<>();
        injectContext(Context.current(), headers);
        return headers;
    }

    // Convenience: get Span from context
    public static Span spanFromContext(Context context) {
        return Span.fromContext(context);
    }

    // Convenience: get SpanContext from context
    public static SpanContext spanContextFromContext(Context context) {
        return Span.fromContext(context).getSpanContext();
    }


}
