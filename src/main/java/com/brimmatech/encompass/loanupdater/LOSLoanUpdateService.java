package com.brimmatech.encompass.loanupdater;

import com.brimmatech.docflow.v2.dto.LoanUpdateDTO;
import com.brimmatech.docflow.v2.dto.PurchaseAdviceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.oer.Switch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service("encompass-loan-updater")
@RequiredArgsConstructor
@Slf4j
public class LOSLoanUpdateService implements ILoanUpdater {

    @Value("${api.encompass.host}")
    private String encompassBaseUrl;
    @Value("${api.encompass.endpoint.field-writer-uri}")
    private String fieldWriterUrl;
    @Value("${api.encompass.endpoint.enhanced-condition}")
    private String enhancedConditionUrl;
    @Value("${api.encompass.endpoint.standard-condition}")
    private String standardConditionUrl;

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
    public boolean updateLoanByFields(PurchaseAdviceDTO fieldsToBeUpdate, String loanIdentifier, String accessToken) {

        List<LoanUpdateDTO> loanUpdateDTOList = new ArrayList<>();

        fieldsToBeUpdate.getFieldMap().forEach((key, value) -> {

            LoanUpdateDTO loanUpdateDTO = new LoanUpdateDTO();
            loanUpdateDTO.setId(key);
            loanUpdateDTO.setValue(value);
            loanUpdateDTO.setLock(true);

            if (StringUtils.hasText(value) &&
                    !value.equalsIgnoreCase("null")) {
                loanUpdateDTOList.add(loanUpdateDTO);
            }

        });

        webClient.post()
                .uri(fieldWriterUrl, loanIdentifier)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(loanUpdateDTOList).retrieve()
                .bodyToMono(Object.class)
                .block();

        return true;
    }

    @Override
    /**
     * TODO: This should return Either<TaskFailureException, TaskDecision>
     */
    public String updateLoanFields(Map<String,List<LoanUpdateDTO>> updates, String accessToken) {

        Map.Entry<String, List<LoanUpdateDTO>> entry = updates.entrySet().iterator().next();

        String loanId = entry.getKey();
        List<LoanUpdateDTO> value = entry.getValue();

        return webClient.post()
                .uri(fieldWriterUrl, loanId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(value).retrieve()
                .bodyToMono(String.class)
                .block();
    }
    public String updateLoanFields(
            String loanId,
            String conditionId,
            String conditionType,
            JsonNode comments,
            String accessToken
    ) {
        String url;

        if (conditionType == null || conditionType.isBlank()) {
            url = enhancedConditionUrl
                    .replace("{{loanId}}", loanId)
                    .replace("{{ConditionId}}", conditionId);
        } else {
            url = standardConditionUrl
                    .replace("{{loanId}}", loanId)
                    .replace("{{underwriting}}", conditionType)
                    .replace("{{ConditionId}}", conditionId);
        }

        log.info("Updating loan fields  loanId={}, conditionId={}, conditionType={}, url={}",
                loanId, conditionId, conditionType, url);

        String response = webClient.patch()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(comments)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("Successfully updated loan condition. loanId={}, conditionId={}", loanId, conditionId);

        return response;
    }



}
