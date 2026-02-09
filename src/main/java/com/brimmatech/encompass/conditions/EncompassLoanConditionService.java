package com.brimmatech.encompass.conditions;

import com.brimmatech.encompass.conditions.dto.EnhancedConditionUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service("encompass-loan-condition-updater")
@Slf4j
public class EncompassLoanConditionService implements ICondition{

    @Value("${api.encompass.host}")
    private String encompassBaseUrl;

    @Value("${api.encompass.endpoint.enhanced-condition-update-uri}")
    private String enhancedConditionUpdateUrl;

    @Value("${api.encompass.endpoint.post-closing-condition-update-uri}")
    private String postClosingConditionUpdateUrl;

    private WebClient webClient;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        final int size = 16 * 1024 * 1024;

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        webClient = WebClient.builder().baseUrl(encompassBaseUrl).exchangeStrategies(strategies).build();
    }

    @Override
    public void updateEnhancedCondition(String accessToken, String loanIdentifier, String action, List<EnhancedConditionUpdate> request) {
        log.info("Invoking enhanced conditions update url. loanIdentifier: {}", loanIdentifier);

        try {
            webClient.patch()
                    .uri(uriBuilder -> uriBuilder.path(enhancedConditionUpdateUrl)
                            .queryParam("action", action)
                            .build(loanIdentifier))
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception ex) {
            log.error("Exception while updating enhanced condition. loanIdentifier: {}, ErrorMessage: {}", loanIdentifier, ex.getMessage(), ex);
        }
        log.info("Updated enhanced conditions successfully!. loanIdentifier: {}", loanIdentifier);
    }

    @Override
    public void updateEnhancedConditionInPostClosing(String accessToken, String loanIdentifier, String action, List<EnhancedConditionUpdate> request) {
        log.info("Invoking enhanced conditions update url. loanIdentifier: {}", loanIdentifier);

        try {
            webClient.patch()
                    .uri(uriBuilder -> uriBuilder.path(postClosingConditionUpdateUrl)
                            .queryParam("action", action)
                            .build(loanIdentifier))
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (Exception ex) {
            log.error("Exception while updating enhanced condition. loanIdentifier: {}, ErrorMessage: {}", loanIdentifier, ex.getMessage(), ex);
        }
        log.info("Updated enhanced conditions successfully!. loanIdentifier: {}", loanIdentifier);
    }

    @Override
    public ResponseEntity<String> getConditionForLoanNumber(String accessToken, String loanIdentifier) {
        log.info("Invoking get conditions url. loanIdentifier: {}", loanIdentifier);

        try {
            ResponseEntity<String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(enhancedConditionUpdateUrl)
                            .build(loanIdentifier))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(String.class)
                    .block();

            return response;

        } catch (WebClientResponseException ex) {
            log.warn("API returned error. Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());

        } catch (Exception ex) {
            log.error("Exception while getting condition data. loanIdentifier: {}, Error: {}", loanIdentifier, ex.getMessage(), ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred: " + ex.getMessage());
        }
    }


}
