package com.brimmatech.encompass.loanreader;

import com.brimmatech.docflow.exception.UnAuthorizedException;
import com.brimmatech.encompass.attachments.dto.RemoveAttachmentRequest;
import com.brimmatech.encompass.loanreader.encompass.dto.Loan;
import com.brimmatech.encompass.loanreader.fieldReader.FieldReaderResponse;
import com.brimmatech.encompass.loanreader.pipeline.*;
import com.brimmatech.saas.PipelinePaginationRequest;
import com.brimmatech.saas.Term;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("encompass-loanreader")
@NoArgsConstructor
public class EncompassLoanReader implements LoanReader {
    @Value("${api.encompass.host}")
    private String encompassBaseUrl;
    @Value("${api.encompass.endpoint.fetch-loan-uri}")
    private String fetchLoanDetailsUri;

    @Value("${api.encompass.endpoint.fetch-loan}")
    private String fetchLoanUri;
    @Value("${api.encompass.endpoint.simple-pipeline-query}")
    private String pipelineQueryUri;

    @Value("${api.encompass.endpoint.fetch-all-attachments-uri}")
    private String fetchAllAttachments;

    @Value("${api.encompass.endpoint.update-loan-uri}")
    private String updateLoanUri;

    @Value("${api.encompass.endpoint.fetch-users}")
    private String encompassRetrieveUsersUrl;

    @Value("${api.encompass.endpoint.remove-document-attachment}")
    private String removeDocumentAttachmentUri;

    @Value("${api.encompass.endpoint.fetch-efolder}")
    private String encompassRetrieveEfolder;

    private WebClient webClient;

    @PostConstruct
    public void init() {

        final int size = 16 * 1024 * 1024;

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        webClient = WebClient.builder().baseUrl(encompassBaseUrl).exchangeStrategies(strategies).build();
    }

    @Override
    public PipelinePaginationResponse[] fetchAllLoans(List<String> folderNames, String accessToken) {
        Term finalTerms = new Term();
        Set<Term> filterTerms = new HashSet<>();

        folderNames.forEach(folderName -> {
            Term filter = new Term();
            filter.setCanonicalName("Loan.LoanFolder");
            filter.setValue(folderName);
            filter.setMatchType("contains");
            filterTerms.add(filter);
        });

        if (folderNames.size() > 1) {
            finalTerms.setOperator("or");
        }
        finalTerms.setTerms(filterTerms);

        PipelinePaginationRequest pipelinePaginationRequest = new PipelinePaginationRequest();

        pipelinePaginationRequest.setFields(Set.of("Loan.LoanNumber"));
        pipelinePaginationRequest.setFilter(finalTerms);


        PipelinePaginationResponse[] pipelinePaginationResponse = null;

        try {

            pipelinePaginationResponse = webClient.post()
                    .uri(pipelineQueryUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(pipelinePaginationRequest)
                    .retrieve()
                    .bodyToMono(PipelinePaginationResponse[].class)
                    .block();

            log.info("Fetched Matched Folder Loan Details using simple pipeline query");

        } catch (WebClientException e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get loanId for the loanNumber: {}",
                        exception.getResponseBodyAsString(), e);
            } else {
                log.error("Exception thrown when trying to get loanId for the loanNumber: : {}", e.getMessage(), e);
            }
            throw e;
        }
        return pipelinePaginationResponse;
    }

    @Override
    public PipelinePaginationResponse[] fetchLoanGuid(String loanNumber, String accessToken) {

        Filter filter = new Filter();
        filter.setCanonicalName("Loan.LoanNumber");
        filter.setValue(loanNumber);
        filter.setMatchType("exact");

        PipelineRequest pipelineRequest = new PipelineRequest();
        pipelineRequest.setFilter(filter);
        pipelineRequest.setFields(List.of("Loan.LoanNumber", "NextMilestone.MilestoneName", "Fields.MS.Status"));
        pipelineRequest.setIncludeArchivedLoans(true);

        PipelinePaginationResponse[] pipelinePaginationResponse = null;

        try {

            pipelinePaginationResponse = webClient.post()
                    .uri(pipelineQueryUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(pipelineRequest)
                    .retrieve()
                    .bodyToMono(PipelinePaginationResponse[].class)
                    .block();

            log.info("Fetched Loan Guid Details using simple pipeline query");

        } catch (WebClientException e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get loanId for the loanNumber: {}",
                        exception.getResponseBodyAsString(), e);
            } else {
                log.error("Exception thrown when trying to get loanId for the loanNumber: : {}", e.getMessage(), e);
            }
            throw e;
        }
        return pipelinePaginationResponse;
    }

    @Override
    public PipelinePaginationResponse[] fetchMatchedLoan(String loanNumber, String accessToken) {

        Filter filter = new Filter();
        filter.setCanonicalName("Loan.LoanNumber");
        filter.setValue(loanNumber);
        filter.setMatchType("contains");

        PipelineRequest pipelineRequest = new PipelineRequest();
        pipelineRequest.setFilter(filter);
        pipelineRequest.setFields(List.of("Loan.LoanNumber", "NextMilestone.MilestoneName", "Fields.MS.Status"));
        pipelineRequest.setIncludeArchivedLoans(true);

        PipelinePaginationResponse[] pipelinePaginationResponse = null;

        try {

            pipelinePaginationResponse = webClient.post()
                    .uri(pipelineQueryUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(pipelineRequest)
                    .retrieve()
                    .bodyToMono(PipelinePaginationResponse[].class)
                    .block();

            log.info("Fetched Loan Guid Details using simple pipeline query");

        } catch (WebClientException e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get loanId for the loanNumber: {}",
                        exception.getResponseBodyAsString(), e);
            } else {
                log.error("Exception thrown when trying to get loanId for the loanNumber: : {}", e.getMessage(), e);
            }
            throw e;
        }
        return pipelinePaginationResponse;
    }


    @Override
    public List<FieldReaderResponse> fetchLoanDetails(String loanGuid, List<String> sorFieldIds, String accessToken) {

        Map<String, String> fieldReaderResponse = null;
        try {

            fieldReaderResponse = webClient.post()
                    .uri(fetchLoanDetailsUri, loanGuid)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(sorFieldIds)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                    })
                    .block();

            log.info("Fetched Loan Guid Details using simple pipeline query");

            return fieldReaderResponse != null ? fieldReaderResponse.entrySet()
                    .stream()
                    .map(entry -> {
                        FieldReaderResponse response = new FieldReaderResponse();
                        response.setFieldId(entry.getKey());
                        response.setValue(entry.getValue());
                        return response;
                    })
                    .collect(Collectors.toList())
                    : Collections.emptyList();


        } catch (WebClientException e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get loanDetails for the loan using fieldReader : {}",
                        exception.getResponseBodyAsString(), e);
            } else {
                log.error("Exception thrown when trying to get loanDetails for the loan using fieldReader : {}",
                        e.getMessage(),
                        e);
            }
            throw e;
        }
    }

    @Override
    public Loan fetchLoan(String loanGuid, String accessToken) {

        Loan loan = null;

        try {

            loan = webClient.get()
                    .uri(fetchLoanUri, loanGuid)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(Loan.class)
                    .block();

            log.info("Fetched Loan Guid Details using simple pipeline query");

        } catch (WebClientException e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get loanDetails for the loan : {}",
                        exception.getResponseBodyAsString(), e);
            } else {
                log.error("Exception thrown when trying to get loanDetails for the loan : {}", e.getMessage(), e);
            }
            throw e;
        }

        return loan;
    }

    @Override
    public Loan updateLoan(String loanGuid, JsonNode loanData, String accessToken) {
        Loan loan = null;
        if (!loanData.isEmpty()) {
            try {

                loan = webClient.patch()
                        .uri(updateLoanUri, loanGuid)
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .bodyValue(loanData)
                        .retrieve()
                        .bodyToMono(Loan.class)
                        .block();

                log.info("Fetched Loan Guid Details using simple pipeline query");

            } catch (WebClientException e) {
                if (e instanceof WebClientResponseException exception) {
                    log.error("Exception thrown when trying to update loanDetails for the loan : {}",
                            exception.getResponseBodyAsString(), e);
                } else {
                    log.error("Exception thrown when trying to update loanDetails for the loan : {}",
                            e.getMessage(),
                            e);
                }
                throw e;
            }

        }
        return loan;
    }

    @Override
    public List<EncompassUserResponse> fetchUsers(String accessToken) {

        List<EncompassUserResponse> encompassUserResponse = null;

        try {

            encompassUserResponse = webClient.get()
                    .uri(encompassRetrieveUsersUrl)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<EncompassUserResponse>>() {
                    })
                    .block();

            log.info("Fetched Loan Guid Details using simple pipeline query");

        } catch (WebClientException e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get loanDetails for the loan : {}",
                        exception.getResponseBodyAsString(), e);
            } else {
                log.error("Exception thrown when trying to get loanDetails for the loan : {}", e.getMessage(), e);
            }
            throw e;
        }

        return encompassUserResponse;
    }

    @Override
    public List<PipelinePaginationResponse> fetchEligibleLoans(PipelinePaginationRequest pipelineRequest,
                                                               String accessToken) {
        List<PipelinePaginationResponse> allLoans = new ArrayList<>();
        int start = 0;
        int limit = 1000;
        boolean hasMore = true;

        try {
            String token = accessToken.replace("Bearer", "").trim();

            log.info("pipelinerequest:{}", pipelineRequest);

            while (hasMore) {
                final int currentStart = start;

                List<PipelinePaginationResponse> batch = webClient.post()
                        .uri(uriBuilder -> uriBuilder.path(pipelineQueryUri)
                                .queryParam("start", currentStart)
                                .queryParam("limit", limit)
                                .build())
                        .headers(headers -> headers.setBearerAuth(token))
                        .bodyValue(pipelineRequest)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<PipelinePaginationResponse>>() {
                        })
                        .block();

                if (batch == null || batch.isEmpty()) {
                    log.info("No more loans to fetch. Stopping pagination.");
                    break;
                }

                allLoans.addAll(batch);
                log.info("Fetched {} loans in this batch. Total so far: {}", batch.size(), allLoans.size());

                if (batch.size() < limit) {
                    hasMore = false;
                } else {
                    start += limit;
                }
            }

            log.info("Completed fetching loans. Total fetched: {}", allLoans.size());
            return allLoans;

        } catch (Exception e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception while fetching loans: {}", exception.getResponseBodyAsString(), e);
                if (exception.getStatusCode().equals(HttpStatusCode.valueOf(401)) ||
                        exception.getStatusCode().equals(HttpStatusCode.valueOf(403))) {
                    throw new UnAuthorizedException("Token is expired.", e);
                }
            } else {
                log.error("Unexpected error while fetching loans: {}", e.getMessage(), e);
            }
            throw e;
        }
    }


    @Override
    public List<PipelinePaginationResponse> fetchModifiedLoans(String accessToken, List<String> loanNumbers) {

        log.info("Starting to fetch modified loans using simple pipeline query. Loan count: {}, Loan numbers: {}",
                loanNumbers.size(),
                loanNumbers);

        FilterV2 filter = new FilterV2();
        filter.setCanonicalName("Loan.LoanNumber");
        filter.setMatchType("Multivalue");
        filter.setValues(loanNumbers);

        PipelineRequestV2 pipelineRequest = new PipelineRequestV2();
        pipelineRequest.setFilter(filter);
        pipelineRequest.setFields(List.of("Loan.LoanNumber", "Loan.LoanFolder"));
        pipelineRequest.setIncludeArchivedLoans(true);

        List<PipelinePaginationResponse> pipelinePaginationResponse = null;
        try {
            String token = accessToken.replace("Bearer", "").trim();
            pipelinePaginationResponse = webClient.post()
                    .uri(pipelineQueryUri)
                    .headers(headers -> headers.setBearerAuth(token))
                    .bodyValue(pipelineRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<PipelinePaginationResponse>>() {
                    })
                    .block();
            log.info("Fetched the modified loan details using simple pipeline query");
        } catch (Exception e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get modified loans {}",
                        exception.getResponseBodyAsString(), e);

                if (exception.getStatusCode().equals(HttpStatusCode.valueOf(401)) ||
                        exception.getStatusCode().equals(HttpStatusCode.valueOf(403))) {
                    throw new UnAuthorizedException("Token is Expired ....", e);
                }
            } else {
                log.error("Exception thrown when trying to get modified loans: : {}", e.getMessage(), e);
            }
            throw e;
        }
        return pipelinePaginationResponse;
    }

    @Override
    public PipelinePaginationResponse[] fetchApprovalLoan(PipelineRequest pipelineRequest, String accessToken) {
        PipelinePaginationResponse[] pipelinePaginationResponse = null;

        try {

            pipelinePaginationResponse = webClient.post()
                    .uri(pipelineQueryUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(pipelineRequest)
                    .retrieve()
                    .bodyToMono(PipelinePaginationResponse[].class)
                    .block();

            log.info("Fetched Loan Guid Details using simple pipeline query");

        } catch (WebClientException e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception thrown when trying to get loanId for the loanNumber: {}",
                        exception.getResponseBodyAsString(), e);
            } else {
                log.error("Exception thrown when trying to get loanId for the loanNumber: : {}", e.getMessage(), e);
            }
            throw e;
        }
        return pipelinePaginationResponse;
    }

    public List<String> getEFolderDocumentNames(String accessToken) {

        return webClient.get()
                .uri(encompassRetrieveEfolder)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .map(node -> node.path("name").asText())
                .collectList()
                .block();
    }


    //COPIED from com.brimmatech.encompass.attachments.EncompassAttachmentProcessor.retrieveDocumentAttachment
    public void removeDocumentAttachmentSourcePackage(String loanGuid, String attachmentId, String accessToken) {
        log.info("Remove encompass document attachment. loanGuid: {}, attachmentId: {}", loanGuid, attachmentId);

        RemoveAttachmentRequest removeAttachmentRequest = new RemoveAttachmentRequest();
        removeAttachmentRequest.setId(attachmentId);


        webClient.patch()
                .uri(removeDocumentAttachmentUri, loanGuid)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(List.of(removeAttachmentRequest))
                .retrieve()
                .toBodilessEntity()
                .block();

        log.info("Removed encompass document attachment successfully!. loanGuid: {}, attachmentId: {}",
                loanGuid,
                attachmentId);
    }

}
