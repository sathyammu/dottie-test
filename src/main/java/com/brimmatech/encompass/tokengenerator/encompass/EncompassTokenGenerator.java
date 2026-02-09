package com.brimmatech.encompass.tokengenerator.encompass;

import com.brimmatech.docflow.dfcommon.Tenant.TenantCredentials;
import com.brimmatech.encompass.tokengenerator.TokenGenerator;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service("encompass")
@Slf4j
public class EncompassTokenGenerator implements TokenGenerator {

    @Value("${api.encompass.host}")
    private String encompassBaseUrl;

    @Value("${api.encompass.endpoint.token-uri}")
    private String encompassAuthTokenUrl;

    @Override
    public TokenResponse defaultTokenGenerator(TenantCredentials tenant) {
        TokenResponse response = null;
        WebClient webClient = WebClient.builder().baseUrl(encompassBaseUrl).build();
        try {
            String encompassUsername = String.format("%s@encompass:%s",
                    tenant.getUserName(),
                    tenant.getInstanceId());

            response =  webClient.post()
                    .uri(encompassAuthTokenUrl)
                    .body(BodyInserters.fromFormData("grant_type", "password")
                            .with("username", encompassUsername)
                            .with("password", tenant.getPassword())
                            .with("client_id", tenant.getClientId())
                            .with("client_secret", tenant.getClientSecret()))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();
            log.info("Generated encompass token successfully by calling ellimae API");
        } catch(WebClientException e){
            if(e instanceof WebClientResponseException exception){
                log.error("Exception thrown when trying to generate token for the tenant : {}",
                        exception.getResponseBodyAsString(), e);
            }else {
                log.error("Exception thrown when trying to generate token for the tenant : {}", e.getMessage(), e);
            }
            throw e;
        }
        return response;
    }

    @Override
    public TokenResponse clientTokenGenerator(TenantCredentials tenant) {

        TokenResponse response = null;

        WebClient webClient = WebClient.builder().baseUrl(encompassBaseUrl).build();

        try{
            response = webClient.post().uri(encompassAuthTokenUrl)
                    .body(BodyInserters.fromFormData("client_id", tenant.getClientId())
                            .with("client_secret", tenant.getClientSecret())
                            .with("instance_id", tenant.getInstanceId())
                            .with("scope", "lp").with("grant_type", "client_credentials"))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();
            log.info("Generated encompass token after calling the client API");
        }catch(WebClientException e){
            if(e instanceof WebClientResponseException exception){
                log.error("Exception thrown when trying to generate token for the tenant : {}",
                        exception.getResponseBodyAsString(), e);
            }else {
                log.error("Exception thrown when trying to generate token for the tenant : {}", e.getMessage(), e);
            }
            throw e;
        }
        return response;
    }

    @Override
    public TokenResponse subjectImpersonationToken(String accessToken, String userId,TenantCredentials tenantCredentials) {
        TokenResponse response = null;

        WebClient webClient = WebClient.builder().baseUrl(encompassBaseUrl).build();

        try{
            response = webClient.post().uri(encompassAuthTokenUrl)
                    .body(BodyInserters.fromFormData("actor_token", accessToken)
                            .with("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                            .with("actor_token_type", "urn:ietf:params:oauth:token-type:access_token")
                            .with("subject_user_id",userId)
                            .with("scope", "lp").with("client_id", tenantCredentials.getClientId())
                            .with("client_secret", tenantCredentials.getClientSecret()))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();
            log.info("Generated encompass token after calling the client API");
        }catch(WebClientException e){
            if(e instanceof WebClientResponseException exception){
                log.error("Exception thrown when trying to generate token for the tenant : {}",
                        exception.getResponseBodyAsString(), e);
            }else {
                log.error("Exception thrown when trying to generate token for the tenant : {}", e.getMessage(), e);
            }
            throw e;
        }
        return response;
    }
}
