package com.brimmatech.encompass.attachments;

import com.brimmatech.docflow.exception.LoanLockException;
import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.brimmatech.encompass.attachments.dto.EncompassAttachmentV3Response;
import com.brimmatech.encompass.attachments.dto.ReassignFolderAttachmentRequest;
import com.brimmatech.encompass.attachments.dto.RemoveAttachmentRequest;
import com.brimmatech.general.errorhandling.ValliaDataException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service("encompass-attachment-processor")
@Slf4j
public class EncompassAttachmentProcessor implements AttachmentProcessor {

    private static final Pattern NON_UTF8_PATTERN = Pattern.compile("[^\\x00-\\x7F]");
    private static final Pattern SMART_QUOTES_PATTERN = Pattern.compile("[\\u2018\\u2019\\u201C\\u201D]");
    @Autowired
    ObjectMapper objectMapper;
    @Value("${api.encompass.host}")
    private String encompassBaseUrl;
    @Value("${api.encompass.endpoint.fetch-document-attachment}")
    private String fetchDocumentAttachmentUri;
    @Value("${api.encompass.endpoint.fetch-attachment-detail}")
    private String fetchAttachmentDetail;
    @Value("${api.encompass.endpoint.fetch-all-attachments-uri}")
    private String fetchAllAttachments;
    @Value("${api.encompass.endpoint.fetch-attachment-download-url-detail}")
    private String fetchAttachmentDownloadUrlDetail;
    @Value("${api.encompass.endpoint.remove-document-attachment}")
    private String removeDocumentAttachmentUri;
    @Value("${api.encompass.endpoint.v3-assign-document-attachments}")
    private String reassignDocumentUri;
    @Value("${api.encompass.endpoint.create-export-job}")
    private String createExportJobUrl;
    @Value("${api.encompass.endpoint.get-export-job-status}")
    private String getExportJobStatus;
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
    public List<EncompassAttachment> retrieveDocumentAttachment(String loanGuid,
                                                                String documentId,
                                                                String accessToken) {

        List<EncompassAttachment> attachments;
        try {
            String rawJson = webClient.get()
                    .uri(fetchDocumentAttachmentUri, loanGuid, documentId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.debug("Received attachments response: {}", rawJson);

            if (rawJson == null) {
                throw new IllegalStateException("Received null response from server");
            } else {
                attachments = decodeAttachemnt(rawJson);
            }

            log.info("Fetched all document details for a loan :{} ", loanGuid);

        } catch (Exception e) {
            throw new ValliaDataException(e.getMessage());
        }

        return attachments;
    }

    @Override
    public EncompassAttachmentV3Response retrieveAttachmentEntityDetail(String loanGuid, String attachmentId,
                                                                        String accessToken) {

        EncompassAttachmentV3Response encompassAttachmentEntity = null;
        try {

            encompassAttachmentEntity = webClient.get()
                    .uri(fetchAttachmentDetail, loanGuid, attachmentId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(EncompassAttachmentV3Response.class)
                    .block();

            log.info("Fetched all document details for a loan :{} ", loanGuid);

        } catch (Exception e) {
            throw new ValliaDataException(e.getMessage());
        }

        return encompassAttachmentEntity;
    }

    @Override
    public List<EncompassAttachmentV3Response> retrieveAllAttachments(String loanGuid, String accessToken) {

        List<EncompassAttachmentV3Response> encompassAttachments = null;
        try {
            encompassAttachments = webClient.get()
                    .uri(fetchAllAttachments, loanGuid)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<EncompassAttachmentV3Response>>() {
                    })
                    .block();

            log.info("Fetched all attachment details for a loan: {}", loanGuid);

        } catch (Exception e) {
            throw new ValliaDataException(e.getMessage());
        }
        return encompassAttachments;
    }

    @Override
    public EncompassAttachmentDownloadEntity downloadAttachmentUrl(String loanGuid,
                                                                   String accessToken,
                                                                   List<String> attachmentIds) {

        EncompassAttachmentDownloadEntity encompassAttachmentDownloadEntity = null;

        ArrayNode attachmentsArray = objectMapper.createArrayNode();

        attachmentIds.forEach(attachmentsArray::add);

        try {

            encompassAttachmentDownloadEntity = webClient.post()
                    .uri(fetchAttachmentDownloadUrlDetail, loanGuid)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(objectMapper.createObjectNode().set("attachments", attachmentsArray))
                    .retrieve()
                    .bodyToMono(EncompassAttachmentDownloadEntity.class)
                    .block();

            log.info("Fetched all document details for a loan :{} ", loanGuid);

        } catch (Exception e) {
            throw new ValliaDataException(e.getMessage());
        }

        return encompassAttachmentDownloadEntity;
    }

    @Override
    public void removeDocumentAttachment(String loanGuid, String attachmentId, String accessToken) {
        log.info("Remove encompass document attachment. loanGuid: {}, attachmentId: {}", loanGuid, attachmentId);

        RemoveAttachmentRequest removeAttachmentRequest = new RemoveAttachmentRequest();
        removeAttachmentRequest.setId(attachmentId);

        try {
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
        } catch (Exception e) {
            if (e instanceof WebClientResponseException exception) {
                log.error("Exception while fetching loans: {}", exception.getResponseBodyAsString(), e);
                if (exception.getStatusCode().equals(HttpStatusCode.valueOf(409))) {
                    throw new LoanLockException(e.getMessage());
                }
            }
            throw new ValliaDataException(e.getMessage());
        }
    }

    @Override
    public List<EncompassAttachmentDownloadEntityBatch> downloadAttachmentUrlBatchWise(String loanGuid,
                                                                                       String accessToken,
                                                                                       Map<String, String> attachmentIdsWithRequestIdMap) {
        log.info("Starting downloadAttachmentUrl for loan: {}", loanGuid);

        try {

            ArrayNode exportMetaArray = objectMapper.createArrayNode();

            attachmentIdsWithRequestIdMap.forEach((key, value) -> {

                ObjectNode entityNode = objectMapper.createObjectNode();
                entityNode.put("entityId", key);
                entityNode.put("entityType", "urn:elli:encompass:attachment");

                ObjectNode metaNode = objectMapper.createObjectNode();
                metaNode.put("requestId", value);
                metaNode.set("entities", objectMapper.createArrayNode().add(entityNode));

                exportMetaArray.add(metaNode);

            });

            ObjectNode exportEntityNode = objectMapper.createObjectNode();
            exportEntityNode.set("exportMeta", exportMetaArray);

            ObjectNode annotationSettingsMode = objectMapper.createObjectNode();
            annotationSettingsMode.set("visibility",
                    objectMapper.valueToTree(List.of("Private", "Public", "Internal")));

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("requestId", "BatchExportRequest");
            requestBody.set("annotationSettings", annotationSettingsMode);
            requestBody.set("exportEntity", exportEntityNode);

            String apiUrl = UriComponentsBuilder
                    .fromHttpUrl(encompassBaseUrl)
                    .path(createExportJobUrl)
                    .queryParam("skipPersonaChecks", false)
                    .queryParam("includeNotActive", true)
                    .buildAndExpand(loanGuid)
                    .toUriString();

            List<EncompassAttachmentDownloadEntityBatch> responseList = webClient.post()
                    .uri(apiUrl)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<EncompassAttachmentDownloadEntityBatch>>() {
                    })
                    .block();

            log.info("Fetched download URLs for loan: {}, attachments: {}",
                    loanGuid,
                    attachmentIdsWithRequestIdMap.size());
            return responseList;

        } catch (Exception e) {
            log.error("Error while fetching download URLs for loan: {}, error: {}", loanGuid, e.getMessage());
            throw new ValliaDataException("Failed to fetch attachment download URLs: " + e.getMessage());
        }
    }

    public AttachmentBatchExportResultDto pollExportJobStatus(
            String exportJobId,
            String accessToken,
            int maxRetries,
            Duration delay) {

        String exportJobUrl = UriComponentsBuilder
                .fromHttpUrl(encompassBaseUrl)
                .path(getExportJobStatus)
                .pathSegment(exportJobId)
                .toUriString();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                AttachmentBatchExportResultDto jobStatus = webClient.get()
                        .uri(exportJobUrl)
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .retrieve()
                        .bodyToMono(AttachmentBatchExportResultDto.class)
                        .block();

                if ("Success".equalsIgnoreCase(jobStatus.getStatus())) {
                    log.info("Export job {} completed with status: {}", exportJobId, jobStatus.getStatus());
                    return jobStatus;
                }

                log.info("Export job {} still queued (attempt {}/{}), retrying after {} sec...",
                        exportJobId, attempt, maxRetries, delay.getSeconds());
                Thread.sleep(delay.toMillis());

            } catch (Exception e) {
                log.warn("Error polling export job {}: {}", exportJobId, e.getMessage());
            }
        }

        log.error("Export job {} did not complete after {} attempts", exportJobId, maxRetries);
        throw new ValliaDataException("Export job did not complete in time: " + exportJobId);
    }

    public List<EncompassAttachment> decodeAttachemnt(String rawJson) throws Exception {
        byte[] bytes = rawJson.getBytes(StandardCharsets.ISO_8859_1);
        String utf8Json = new String(bytes, StandardCharsets.UTF_8);


        utf8Json = SMART_QUOTES_PATTERN.matcher(utf8Json).replaceAll("\"");


        utf8Json = NON_UTF8_PATTERN.matcher(utf8Json).replaceAll("");


        return objectMapper.readValue(utf8Json, new TypeReference<List<EncompassAttachment>>() {
        });
    }

    public void reassignDocumentForAttachment(ReassignFolderAttachmentRequest reassignFolderAttachmentRequest,
                                              String accessToken) {


        webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path(reassignDocumentUri)
                        .queryParam("action", "add")
                        .build(reassignFolderAttachmentRequest.getLoanId(),
                                reassignFolderAttachmentRequest.getDocumentId())

                )
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(objectMapper.valueToTree(List.of(reassignFolderAttachmentRequest)))

                .retrieve()
                .onStatus(
                        status -> status == HttpStatus.BAD_REQUEST,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("400 response: {}", body);
                                    return Mono.error(new IllegalArgumentException(body));
                                })
                )
                .toBodilessEntity()

                .block();
    }
}
