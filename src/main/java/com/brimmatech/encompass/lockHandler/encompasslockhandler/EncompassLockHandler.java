package com.brimmatech.encompass.lockHandler.encompasslockhandler;

import com.brimmatech.docflow.exception.LoanLockException;
import com.brimmatech.encompass.lockHandler.LoanLockHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service("encompass-lock-handler")
@Slf4j
public class EncompassLockHandler implements LoanLockHandler {

    @Value("${api.encompass.host}")
    private String encompassBaseUrl;

    @Value("${api.encompass.endpoint.retrieve-lock-uri}")
    private String retrieveLocksUri;

    @Value("${api.encompass.endpoint.unlock-resource-uri}")
    private String unlockResourceUri;

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
    public List<EncompassLockResponse> retrieveLock(String accessToken,String loanGuid) {

        List<EncompassLockResponse> lockResponse = null;
        try {

            lockResponse =  webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(retrieveLocksUri)
                            .queryParam("resourceType","loan")
                            .queryParam("resourceId",loanGuid)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<EncompassLockResponse>>() {
                    })
                    .block();

            log.info("");

        } catch (Exception e) {
            log.error(":{}", e.getMessage());
        }

        return lockResponse;
    }

    @Override
    public LockResourceResponse lockResource(String accessToken, String loanGuid) {

        LockResourceResponse lockResourceResponse;

        EncompassLockResourceRequest lockResourceRequest = constructEncompassLockResourceRequest(loanGuid);

        try {
            lockResourceResponse = webClient.post()
                    .uri(uriBuilder ->  uriBuilder
                            .path(retrieveLocksUri)
                            .queryParam("view", "id")
                            .build())
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                    .bodyValue(lockResourceRequest)
                    .retrieve()
                    .bodyToMono(LockResourceResponse.class)
                    .block();

            log.info("Resource locked successfully!. loanGuid: {}", loanGuid);

        } catch (Exception ex) {
            log.error(":{}", ex.getMessage());
            throw new LoanLockException(ex.getMessage());
        }

        return lockResourceResponse;
    }

    @Override
    public void unlockResource(String accessToken,String lockId, String loanGuid) {

        try {
                webClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path(unlockResourceUri)
                            .queryParam("resourceType","loan")
                            .queryParam("resourceId",loanGuid).build(lockId))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("");

        } catch (Exception e) {
            log.error(":{}", e.getMessage());
        }
    }

    private EncompassLockResourceRequest constructEncompassLockResourceRequest(String loanGuid) {

        EncompassLockResourceRequest encompassLockResourceRequest = new EncompassLockResourceRequest();

        Resource resource = new Resource();
        resource.setEntityId(loanGuid);
        resource.setEntityType("loan");

        encompassLockResourceRequest.setResource(resource);
        encompassLockResourceRequest.setLockType("shared");

        return encompassLockResourceRequest;
    }

}
