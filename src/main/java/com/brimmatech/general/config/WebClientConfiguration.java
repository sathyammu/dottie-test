package com.brimmatech.general.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration @Slf4j @RequiredArgsConstructor public class WebClientConfiguration {

    public static final int MAX_SIZE_MB = 2 * 1024;
    public static final List<@NotNull HttpStatus> RETRIABLE_STATUSUES
            = List.of(HttpStatus.REQUEST_TIMEOUT,
            HttpStatus.TOO_EARLY,
            HttpStatus.TOO_MANY_REQUESTS,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.GATEWAY_TIMEOUT);
    private final Supplier<ZonedDateTime> nowUtcZoned;

    private static boolean isRetryableStatusCode(ClientResponse clientResponse) {
        return RETRIABLE_STATUSUES.contains(HttpStatus.valueOf(clientResponse.statusCode().value()));
    }

    @Bean public WebClient encompassWebClient() {
        String connectionProviderName = "encompassApiConnectionProvider";
        return createWebclient(connectionProviderName, retryableFilterForGetMethods(), b -> {
            b.codecs(builder -> builder.defaultCodecs().maxInMemorySize(MAX_SIZE_MB * 1024));
        });
    }

    @Bean public WebClient genericWebclient() {
        String connectionProviderName = "genericApiConnectionProvider";
        return createWebclient(connectionProviderName, retryableFilterForGetMethods(), (b) -> {
            final int size = (int) DataSize.ofMegabytes(16).toBytes();
            final ExchangeStrategies
                    strategies =
                    ExchangeStrategies.builder().codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size)).build();
            b.exchangeStrategies(strategies);
        });
    }

    @Bean public WebClient bigResponseWebclient() {
        String connectionProviderName = "genericWebclient";
        final int size = (int) DataSize.ofMegabytes(16).toBytes();
        final ExchangeStrategies
                strategies =
                ExchangeStrategies.builder().codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size)).build();
        return createWebclient(connectionProviderName,
                retryableFilterForGetMethods(),
                (builder) -> builder.exchangeStrategies(strategies));
    }

    @Bean public WebClient webhookPoster() {
        String connectionProviderName = "webhookPoster";
        return createWebclient(connectionProviderName, retryableFilterForPostMethod(), null);
    }

    @NotNull
    private WebClient createWebclient(String connectionProviderName,
                                      ExchangeFilterFunction filter,
                                      Consumer<WebClient.Builder> consumer) {
        int maxConnections = 5;
        HttpClient httpClient = HttpClient.create(ConnectionProvider

                        .builder(connectionProviderName).maxConnections(maxConnections).build())
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS)))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000 * 30)
                .responseTimeout(Duration.ofSeconds(30))

                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
        WebClient.Builder
                builder =
                WebClient.builder().filter(filter).clientConnector(new ReactorClientHttpConnector(httpClient));
        if (Objects.nonNull(consumer)) {
            consumer.accept(builder);
        }
        return builder.build();
    }

    private ExchangeFilterFunction retryableFilterForGetMethods() {

        return (request, next) -> next.exchange(request)
                .flatMap(clientResponse -> {
                    if (request.method().equals(HttpMethod.GET) && isRetryableStatusCode(clientResponse)) {
                        return clientResponse.createException().flatMap(Mono::error);
                    }
                    return Mono.just(clientResponse);
                })
                .retryWhen(Retry.backoff(30, Duration.ofSeconds(3))
                        .doBeforeRetry(retrySignal -> log.debug("Retrying {} {} due to",
                                request.method(),
                                request.url(),
                                retrySignal.failure())));
    }

    private ExchangeFilterFunction retryableFilterForPostMethod() {

        return (request, next) -> next.exchange(request).flatMap(clientResponse -> {
            if (request.method().equals(HttpMethod.POST) && isRetryableStatusCode(clientResponse)) {
                return clientResponse.createException().flatMap(Mono::error);
            }
            return Mono.just(clientResponse);

        });

    }

    @Bean Function<WebClientResponseException, Boolean> statusChecker() {
        return ex -> ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429;

    }


}
