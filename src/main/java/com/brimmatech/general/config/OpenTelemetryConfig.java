package com.brimmatech.general.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenTelemetryConfig {

    @Value("${app.zipkin.host}")
    private String zipkinHostUrl;


    @Value("${info.app.id}")
    private String appId;

    @Value("${info.app.deploy_environment}")
    private String appDeployEnv;


    private String serviceName(String trackerType) {
        return String.format("%s.%s.%s", trackerType, appId, appDeployEnv);
    }


    @Bean("apiOpenTelemetry")
    public OpenTelemetry apiOpenTelemetry() {
        return buildOpenTelemetry(serviceName("api"));
    }

    @Bean("azureAiOpenTelemetry")
    public OpenTelemetry azureAiOpenTelemetry() {
        return buildOpenTelemetry(serviceName("azure-ai"));
    }

    @Bean("defaultOpenTelemetry")
    public OpenTelemetry defaultOpenTelementary() {
        return buildOpenTelemetry(serviceName("docflow"));
    }

    @Bean("apiTracer")
    public Tracer apiTracer(@Qualifier("apiOpenTelemetry") OpenTelemetry otel) {
        return otel.getTracer("com.brimmatech.api");
    }

    @Bean("azureAiTracer")
    public Tracer azureAiTracer(@Qualifier("azureAiOpenTelemetry") OpenTelemetry otel) {
        return otel.getTracer("com.brimmatech.azure-ai");
    }

    @Bean
    @Primary
    @Qualifier("defaultTracer")
    public Tracer defaultTracer(@Qualifier("defaultOpenTelemetry") OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.brimmatech.docflow.default");
    }


    private OpenTelemetry buildOpenTelemetry(String serviceName) {
        SpanExporter zipkinExporter = ZipkinSpanExporter.builder()
                .setEndpoint(zipkinHostUrl)
                .build();

        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(zipkinExporter).build();

        Resource resource = Resource.create(
                Attributes.of(AttributeKey.stringKey("service.name"), serviceName)
        );

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(spanProcessor)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
    }

}
