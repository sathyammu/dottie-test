package com.brimmatech.docflow.v2.tracing;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.HashMap;
import java.util.Map;

public class TracingContextPropagator {

    private static final W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();

    // Serialize current trace context into a map (to save in DB with the task)
    public static Map<String, String> serializeContext() {
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(Context.current(), carrier, MapSetter.INSTANCE);
        return carrier;
    }

    // Deserialize from DB back to OTel Context
    public static Context deserializeContext(Map<String, String> carrier) {
        return propagator.extract(Context.current(), carrier, MapGetter.INSTANCE);
    }

    private enum MapSetter implements TextMapSetter<Map<String, String>> {
        INSTANCE;

        @Override
        public void set(Map<String, String> carrier, String key, String value) {
            carrier.put(key, value);
        }
    }

    private enum MapGetter implements TextMapGetter<Map<String, String>> {
        INSTANCE;

        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    }
}
