package com.brimmatech.docflow.v2.tracing;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Mono;

import java.util.Map;

public class ZipkinTracingPolicy implements HttpPipelinePolicy {

    private final Tracer tracer;
    private final Map<String, String> customAttributes;
    private final Span parentSpan;

    public ZipkinTracingPolicy(Tracer tracer, Map<String, String> customAttributes, Span parentSpan) {
        this.tracer = tracer;
        this.customAttributes = customAttributes;
        this.parentSpan = parentSpan;
    }

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext httpPipelineCallContext, HttpPipelineNextPolicy httpPipelineNextPolicy) {

        String spanName = "azure-ai: " + httpPipelineCallContext.getHttpRequest().getHttpMethod()
                + " " + httpPipelineCallContext.getHttpRequest().getUrl().getPath();

        Span span = tracer.spanBuilder(spanName)
                .setParent(io.opentelemetry.context.Context.current().with(parentSpan))
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {

            span.setAttribute("http.method", httpPipelineCallContext.getHttpRequest().getHttpMethod().toString());
            span.setAttribute("http.url", httpPipelineCallContext.getHttpRequest().getUrl().toString());

            if (customAttributes != null) {
                customAttributes.forEach(span::setAttribute);
            }

            return httpPipelineNextPolicy.process()
                    .doOnSuccess(response -> {
                        span.setAttribute("http.status_code", response.getStatusCode());
                        span.end();
                    })
                    .doOnError(error -> {
                        span.recordException(error);
                        span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Azure SDK call failed");
                        span.end();
                    });
        }
    }
}
