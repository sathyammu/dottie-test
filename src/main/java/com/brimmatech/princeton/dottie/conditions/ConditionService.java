package com.brimmatech.princeton.dottie.conditions;

import com.brimmatech.docflow.common.beanhelper.BeanHelper;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.exception.LoanLockException;
import com.brimmatech.docflow.v2.services.AzureBlobService;
import com.brimmatech.docflow.v2.services.DocIntelHelperService;
import com.brimmatech.docflow.v2.task.TaskFactory;
import com.brimmatech.encompass.attachments.EncompassAttachmentProcessor;
import com.brimmatech.encompass.attachments.dto.AttachmentUpdate;
import com.brimmatech.encompass.conditions.ConditionType;
import com.brimmatech.encompass.conditions.ICondition;
import com.brimmatech.encompass.conditions.dto.*;
import com.brimmatech.encompass.documentupload.EncompassDocumentUploadService;
import com.brimmatech.encompass.documentupload.FileUploadDetail;
import com.brimmatech.encompass.documentupload.UploadAttachment;
import com.brimmatech.encompass.loanreader.LoanReader;
import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import com.brimmatech.encompass.lockHandler.LoanLockService;
import com.brimmatech.encompass.lockHandler.encompasslockhandler.LockResourceResponse;
import com.brimmatech.general.errorhandling.ValliaDataException;
import com.brimmatech.princeton.dottie.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.brimmatech.encompass.conditions.ConditionType.*;

@Service
@Slf4j
public class ConditionService {
    private final BeanHelper beanHelper;

    private final LoanLockService loanLockService;

    private final TaskFactory taskFactory;

    private final ObjectMapper objectMapper;

    private final TenantEntityRepository tenantEntityRepository;

    private final EncompassDocumentUploadService encompassDocumentUploadService;

    private final AzureBlobService azureBlobService;

    private final DocIntelHelperService docIntelHelperService;

    private final EncompassAttachmentProcessor encompassAttachmentProcessor;

    @Value("${api.encompass.host}")
    private String encompassBaseUrl;

    private static final String STEP_UPDATE = "UPDATE_CONDITION_STATUS";

    private static final String ASSIGN_DOCUMENTS = "ASSIGN_DOCUMENTS";

    private static final String UPLOAD_ATTACHMENT = "UPLOAD_ATTACHMENT";

    private static final String REMOVE_ATTACHMENT = "REMOVE_ATTACHMENT";

    public ConditionService(BeanHelper beanHelper, LoanLockService loanLockService, TaskFactory taskFactory, ObjectMapper objectMapper,
                            TenantEntityRepository tenantEntityRepository, EncompassDocumentUploadService encompassDocumentUploadService,
                            AzureBlobService azureBlobService, DocIntelHelperService docIntelHelperService, EncompassAttachmentProcessor encompassAttachmentProcessor) {
        this.beanHelper = beanHelper;
        this.loanLockService = loanLockService;
        this.taskFactory = taskFactory;
        this.objectMapper = objectMapper;
        this.tenantEntityRepository = tenantEntityRepository;
        this.encompassDocumentUploadService = encompassDocumentUploadService;
        this.azureBlobService = azureBlobService;
        this.docIntelHelperService = docIntelHelperService;
        this.encompassAttachmentProcessor = encompassAttachmentProcessor;
    }

    public void updateEnhancedCondition(String accessToken, TenantEntity tenant, String action,
                                        EnhancedConditionUpdateRequest enhancedConditionUpdates) {

        if (StringUtils.hasText(enhancedConditionUpdates.getLoanNumber())) {

            String loanIdentifier = "";

            if (StringUtils.hasText(action)) {

                LoanReader loanReader = beanHelper
                        .getLoanReaderBean(tenant.getSor().getSystemOfRecordName() + "-loanreader");

                PipelinePaginationResponse[] pipelinePaginationResponse = loanReader
                        .fetchLoanGuid(enhancedConditionUpdates.getLoanNumber(), accessToken);

                loanIdentifier = pipelinePaginationResponse[0].getLoanId();

                log.info("Update enhanced conditions. loanIdentifier: {}, action: {}", loanIdentifier, action);

                ICondition conditionUpdater = beanHelper
                        .getLOSConditionUpdaterBean(tenant.getSor().getSystemOfRecordName() + "-loan-condition-updater");

                LockResourceResponse lockResourceResponse = loanLockService
                        .lockResource(accessToken, loanIdentifier, tenant);

                AtomicInteger count = new AtomicInteger(1);

                List<EnhancedConditionUpdate> modifiedEnhancedConditions = enhancedConditionUpdates.getConditions().stream()
                        .map(condition -> {
                            if (!StringUtils.hasText(condition.getTitle())) {
                                condition.setTitle("Investor_Delivery_Condition_" + count.getAndIncrement());
                            }
                            return condition;
                        }).toList();

                ResponseEntity<String> result = conditionUpdater.getConditionForLoanNumber(accessToken, loanIdentifier);

                int statusCode = result.getStatusCode().value();
                String responseBody = result.getBody();

                if (statusCode == 403 && responseBody.contains("Conditions are not enabled") &&
                        tenant.getEnhancedConditionType().equalsIgnoreCase("Post-Closing")) {

                    Application application = new Application();
                    application.setEntityId("_borrower1");
                    application.setEntityType("Application");

                    modifiedEnhancedConditions.forEach(request-> {
                        request.setApplication(application);
                        request.setSource("Manual");
                        request.setDescription(request.getExternalDescription());
                    });

                    conditionUpdater.updateEnhancedConditionInPostClosing(accessToken, loanIdentifier, action, modifiedEnhancedConditions);

                } else if (statusCode == 200) {
                    if(StringUtils.hasText(tenant.getEnhancedConditionType())){
                        modifiedEnhancedConditions.forEach(request-> request.setConditionType(tenant.getEnhancedConditionType()));
                    }
                    conditionUpdater.updateEnhancedCondition(accessToken, loanIdentifier, action, modifiedEnhancedConditions);
                } else {
                    log.warn("Unexpected status code {} or response for loanIdentifier: {}. Body: {}", statusCode, loanIdentifier, responseBody);
                }

                loanLockService.unlockAResource(accessToken, lockResourceResponse.getId(), loanIdentifier, tenant);

            } else {
                log.error("Enhanced condition API action param value not present. loanIdentifier: {}", loanIdentifier);
            }
        } else {
            log.error("Loan Number is not present in the enhanced condition update request. payload: {}", enhancedConditionUpdates);
        }
    }

    public String updateConditionStatus(String userAccessToken, long tenantId,
                                        String loanId,
                                        String type,
                                        List<ConditionStatusRequest> req) {

        String conditionId = req.getFirst().getId();
        String endpoint = buildEndpoint(type, loanId, conditionId);

        log.info("Updating condition status | tenant={} loanId={} type={} endpoint={} status={}",
                tenantId, loanId, type, endpoint, req);
        WebClient webClient = WebClient.builder().build();

        return webClient.patch()
                .uri(endpoint)
                .headers(headers -> headers.setBearerAuth(userAccessToken))
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String buildEndpoint(String type, String loanId, String conditionId) {

        ConditionType resolvedType = ConditionType.fromString(type);

        return switch (resolvedType) {
            case ENHANCED ->
                    encompassBaseUrl + "encompass/v3/loans/" + loanId + "/conditions/" +conditionId + "/tracking?action=add&view=entity";
            case PRELIMINARY ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId + "/conditions/preliminary?action=update&view=entity";

            case UNDERWRITING ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId + "/conditions/underwriting?action=update&view=id";

            case POSTCLOSING ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId + "/conditions/postclosing?action=update";

            default -> throw new IllegalArgumentException("Invalid condition type: " + type);
        };
    }

    public String loanConditionStatusChange(String userAccessToken, long tenantId,
                                            String loanId,
                                            String type,
                                            List<ConditionStatusRequest> req) {

        log.info("ENCOMPASS_UPDATE_START | loanId={}", loanId);

        try {
            ConditionType resolvedType = ConditionType.fromString(type);

            boolean isEnhanced = resolvedType == ENHANCED;


            if(!isEnhanced){
                updateConditionStatus(userAccessToken, tenantId, loanId, type, req);
            }else {
                updateEnhancedConditionStatuses(userAccessToken, tenantId, loanId, type, req);
            }
            log.info("ENCOMPASS_UPDATE_SUCCESS | loanId={}", loanId);
            return "SUCCESS";

        } catch (WebClientResponseException e) {

            if (e.getStatusCode() == HttpStatus.CONFLICT) {

                log.warn("LOAN_LOCKED | loanId={} schedulingRetry", loanId);

                taskFactory.initiateJobForUpdateEncompass(
                        List.of(req),
                        loanId,
                        tenantId,
                        STEP_UPDATE);

                log.info("RETRY_SCHEDULED | loanId={}", loanId);
                return "LOAN_LOCKED";

            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {

                log.error("ENCOMPASS_BAD_REQUEST | loanId={} response={}",
                        loanId, e.getResponseBodyAsString());
                throw new DocflowDataException(e.getResponseBodyAsString());

            } else {

                log.error("ENCOMPASS_ERROR | loanId={} status={} response={}",
                        loanId, e.getStatusCode(), e.getResponseBodyAsString());
                throw new ValliaDataException(e.getResponseBodyAsString());
            }
        }
    }

    public void updateEnhancedConditionStatuses(String userAccessToken, long tenantId, String loanId, String type,
                                                List<ConditionStatusRequest> requests) {

        WebClient webClient = WebClient.builder().build();

        for (ConditionStatusRequest req : requests) {

            String endpoint = buildEndpoint(type, loanId, req.getId());

            EnhancedConditionStatusUpdateRequest payload =
                    new EnhancedConditionStatusUpdateRequest();
            payload.setStatus(req.getStatus());
            payload.setIsChecked(req.getIsChecked());

            log.info(
                    "Updating enhanced condition | loanId={} conditionId={} status={}",
                    loanId, req.getId(), req.getStatus()
            );

            webClient.patch()
                    .uri(endpoint)
                    .headers(h -> h.setBearerAuth(userAccessToken))
                    .bodyValue(List.of(payload))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        }
    }

    public List<ConditionResponse> fetchConditionDocuments(
            String loanId,
            String type,
            String authorizationHeader) {

        ConditionType resolvedType = ConditionType.fromString(type);

        ArrayNode mergedConditions = objectMapper.createArrayNode();

        if (resolvedType == ConditionType.ENHANCED) {

            JsonNode enhancedConditions =
                    fetchConditions(loanId, ConditionType.ENHANCED.name(), authorizationHeader);

            mergedConditions.addAll((ArrayNode) enhancedConditions);

        } else {

            for (ConditionType standardType : List.of(
                    ConditionType.PRELIMINARY,
                    ConditionType.UNDERWRITING,
                    ConditionType.POSTCLOSING)) {

                JsonNode response =
                        fetchConditions(loanId, standardType.name(), authorizationHeader);

                if (response != null && response.isArray()) {
                    mergedConditions.addAll((ArrayNode) response);
                }
            }
        }

        JsonNode attachments = fetchAttachments(loanId, authorizationHeader);

        Map<String, List<AttachmentResponse>> attachmentLookup =
                buildAttachmentLookup(attachments);

        return buildResponse(mergedConditions, attachmentLookup);
    }

    private JsonNode fetchConditions(String loanId, String type, String authHeader) {

        String url = buildGetConditionsEndpoint(type, loanId);

        String rawResponse = WebClient.builder().build()
                .get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(authHeader))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("Encompass error [{}]: {}", response.statusCode(), body);
                                    return Mono.error(new RuntimeException("Encompass API error"));
                                })
                )
                .bodyToMono(String.class)
                .block();

        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("Empty response from Encompass for loanId={} type={}", loanId, type);
            return JsonNodeFactory.instance.arrayNode();
        }

        try {
            return objectMapper.readTree(rawResponse);
        } catch (Exception e) {
            log.error("Invalid JSON from Encompass [{}]: {}", type, rawResponse);
            return JsonNodeFactory.instance.arrayNode();
        }
    }



    private JsonNode fetchAttachments(String loanId, String authHeader) {

        String url = encompassBaseUrl + "/encompass/v3/loans/" + loanId + "/attachments";

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        WebClient webClient = WebClient.builder()
                .exchangeStrategies(strategies)
                .build();

        return webClient.get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(authHeader))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    private Map<String, List<AttachmentResponse>> buildAttachmentLookup(JsonNode attachments) {

        Map<String, List<AttachmentResponse>> map = new HashMap<>();

        if (attachments != null && attachments.isArray()) {
            for (JsonNode att : attachments) {

                JsonNode assigned = att.get("assignedTo");
                if (assigned == null || !assigned.has("entityId")) continue;

                String documentId = assigned.get("entityId").asText();

                AttachmentResponse attachment = new AttachmentResponse(
                        att.get("id").asText(),
                        att.get("title").asText(),
                        att.path("fileSize").asLong(),
                        att.path("createdDate").asText(null),
                        att.path("createdBy").path("entityName").asText(null)
                );

                map.computeIfAbsent(documentId, k -> new ArrayList<>())
                        .add(attachment);
            }
        }
        return map;
    }

    private List<ConditionResponse> buildResponse(
            JsonNode conditions,
            Map<String, List<AttachmentResponse>> attachmentLookup) {

        List<ConditionResponse> response = new ArrayList<>();

        if (conditions == null || !conditions.isArray()) {
            return response;
        }

        for (JsonNode cond : conditions) {

            List<DocumentResponse> documents = new ArrayList<>();

            // v1 conditions (documents[])
            if (cond.has("documents")) {
                for (JsonNode doc : cond.get("documents")) {
                    String docId = doc.get("entityId").asText();
                    documents.add(new DocumentResponse(
                            docId,
                            doc.get("entityName").asText(),
                            attachmentLookup.getOrDefault(docId, List.of())
                    ));
                }
            }

            // v3 enhanced conditions (assignedTo[])
            if (cond.has("assignedTo")) {
                for (JsonNode ref : cond.get("assignedTo")) {
                    if ("Document".equals(ref.get("entityType").asText())) {
                        String docId = ref.get("entityId").asText();
                        documents.add(new DocumentResponse(
                                docId,
                                ref.get("entityName").asText(),
                                attachmentLookup.getOrDefault(docId, List.of())
                        ));
                    }
                }
            }

            response.add(new ConditionResponse(
                    cond.get("id").asText(),
                    cond.get("title").asText(),
                    cond.get("conditionType").asText(),
                    documents
            ));
        }

        return response;
    }

    private String buildGetConditionsEndpoint(String type, String loanId) {

        ConditionType resolved = ConditionType.fromString(type);

        return switch (resolved) {
            case ENHANCED ->
                    encompassBaseUrl + "/encompass/v3/loans/" + loanId + "/conditions";
            case PRELIMINARY ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId + "/conditions/preliminary";
            case UNDERWRITING ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId + "/conditions/underwriting";
            case POSTCLOSING ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId + "/conditions/postclosing";
            default ->
                    throw new IllegalArgumentException("Invalid condition type: " + type);
        };
    }

    public void addDocumentsToCondition(String loanId, String conditionType, String conditionId, List<ConditionDocumentRequest> documents,
            String authHeader, String action) {

        String endpoint = buildAssignDocumentsEndpoint(conditionType, loanId, conditionId, action);

        log.info("Assigning {} documents to condition {} ({})",
                documents.size(), conditionId, conditionType);

        WebClient webClient = WebClient.builder().build();

        webClient.patch()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(documents)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String buildAssignDocumentsEndpoint(String type, String loanId, String conditionId, String action) {

        ConditionType resolved = ConditionType.fromString(type);

        return switch (resolved) {
            case ENHANCED ->
                    encompassBaseUrl + "/encompass/v3/loans/" + loanId +
                            "/conditions/" + conditionId + "/documents?action=" +action;

            case UNDERWRITING ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId +
                            "/conditions/underwriting/" + conditionId + "/documents?action=" +action;

            case PRELIMINARY ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId +
                            "/conditions/preliminary/" + conditionId + "/documents?action=" +action;

            case POSTCLOSING ->
                    encompassBaseUrl + "/encompass/v1/loans/" + loanId +
                            "/conditions/postclosing/" + conditionId + "/documents?action=" +action;

            default ->
                    throw new IllegalArgumentException("Unsupported condition type: " + type);
        };
    }


    public void assignDocumentsToConditionAndLoanLock(long tenantId, String loanId, String conditionType,
            String conditionId, List<ConditionDocumentRequest> documents, String authHeader, String action) {

        try{
            addDocumentsToCondition(loanId, conditionType, conditionId, documents, authHeader, action);
        }catch (WebClientResponseException e) {

            if (e.getStatusCode() == HttpStatus.CONFLICT) {

                log.warn("ASSIGN_DOCUMENTS: LOAN_LOCKED | loanId={} schedulingRetry", loanId);
                AssignDocumentRequest assignDocumentRequest = new AssignDocumentRequest();
                assignDocumentRequest.setConditionId(conditionId);
                assignDocumentRequest.setConditionType(conditionType);
                assignDocumentRequest.setConditionDocumentRequestList(documents);
                assignDocumentRequest.setAction(action);

                taskFactory.initiateJobForUpdateEncompass(
                        List.of(assignDocumentRequest),
                        loanId,
                        tenantId,
                        ASSIGN_DOCUMENTS);

                log.trace("ASSIGN_DOCUMENTS: RETRY_SCHEDULED | loanId={}", loanId);

            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {

                log.error("ASSIGN_DOCUMENTS: ENCOMPASS_BAD_REQUEST | loanId={} response={}",
                        loanId, e.getResponseBodyAsString());
                throw new DocflowDataException(e.getResponseBodyAsString());

            } else {

                log.error("ASSIGN_DOCUMENTS: ENCOMPASS_ERROR | loanId={} status={} response={}",
                        loanId, e.getStatusCode(), e.getResponseBodyAsString());
                throw new ValliaDataException(e.getResponseBodyAsString());
            }
        }

    }

    public FileUploadDetail uploadAttachment(String accessToken, String tenantId, String loanId, UploadAttachment attachment, MultipartFile file) throws IOException {

        TenantEntity tenant = tenantEntityRepository.findById(Long.parseLong(tenantId))
                .orElseThrow(() -> new DocflowDataException(
                        "Tenant not found for provided tenant_id " + tenantId));

        LockResourceResponse lockResourceResponse = null;
        FileUploadDetail fileUploadDetail;

        try {
            mapFileToAttachment(file, attachment);

            log.info("Locking loan before upload. loanId={}", loanId);

            lockResourceResponse = loanLockService.lockResource(accessToken, loanId, tenant);

            log.trace("Uploading attachment [{}] to document [{}] for loan [{}]", attachment.getAttachmentTitle(), attachment.getDocumentTitle(), loanId);

            fileUploadDetail = encompassDocumentUploadService.uploadAttachmentToLOS(attachment, loanId, accessToken, tenant);

        } catch (LoanLockException ex) {

            log.warn("LOAN_LOCKED | loanId={} schedulingRetry", loanId);

            String sanitizedFileName = docIntelHelperService.generateSanitizedAttachmentName(
                    Objects.requireNonNull(file.getOriginalFilename()));

            String blobTargetLocation = String.format("/dottie/%s/%s", tenant.getTenantName(),
                    sanitizedFileName);

            log.debug("Uploading file to blob location: {}", blobTargetLocation);

            azureBlobService.uploadAttachmentToBlobStorage(file.getBytes(),
                    blobTargetLocation);

            attachment.setAttachmentTitleWithFileExtension(blobTargetLocation);
            attachment.setAttachmentByteArrayData(null);

            taskFactory.initiateJobForUpdateEncompass(List.of(attachment), loanId, Long.parseLong(tenantId), UPLOAD_ATTACHMENT);

            log.trace("RETRY_SCHEDULED | loanId={}", loanId);

            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);

    } finally {
            if (lockResourceResponse != null) {
                log.info("Unlocking loan after upload. loanId={}", loanId);
                loanLockService.unlockAResource(accessToken, lockResourceResponse.getId(), loanId, tenant);
            }
        }
        return fileUploadDetail;
    }

    private void mapFileToAttachment(MultipartFile file, UploadAttachment attachment) {
        try {
            attachment.setAttachmentByteArrayData(file.getBytes());
        } catch (IOException ex) {
            throw new DocflowDataException(
                    "Failed to read attachment file: " + file.getOriginalFilename(), ex);
        }

        attachment.setContentType(file.getContentType());
        attachment.setAttachmentTitleWithFileExtension(file.getOriginalFilename());
    }

    public void updateAttachment(String loanGuid, AttachmentUpdate attachmentUpdate, String accessToken){

        attachmentUpdate.setType(encompassAttachmentProcessor.retrieveAttachmentEntityDetail(loanGuid, attachmentUpdate.getId(), accessToken).getType());

        encompassAttachmentProcessor.updateAttachmentDetails(loanGuid, attachmentUpdate, accessToken);
    }

    public void removeAttachment(String accessToken, String tenantId, String loanId, String attachmentId) {

        TenantEntity tenant = tenantEntityRepository.findById(Long.parseLong(tenantId))
                .orElseThrow(() -> new DocflowDataException(
                        "Tenant not found for provided tenant_id " + tenantId));

        LockResourceResponse lockResourceResponse = null;

        try {

            log.info("Locking loan before remove attachment. loanId={}", loanId);

            lockResourceResponse = loanLockService.lockResource(accessToken, loanId, tenant);

            log.trace("Remove attachment [{}] to document [{}] for loan [{}]", attachmentId, loanId);

            encompassAttachmentProcessor.removeDocumentAttachment(loanId, attachmentId, accessToken);

        } catch (LoanLockException ex) {

            log.warn("LOAN_LOCKED | loanId={} schedulingRetry", loanId);
            UploadAttachment uploadAttachment = new UploadAttachment();
            uploadAttachment.setAttachmentTitle(attachmentId);


            taskFactory.initiateJobForUpdateEncompass(uploadAttachment, loanId, Long.parseLong(tenantId), REMOVE_ATTACHMENT);

            log.trace("RETRY_SCHEDULED | loanId={}", loanId);

            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);

        } finally {
            if (lockResourceResponse != null) {
                log.info("Unlocking loan after remove attachment. loanId={}", loanId);
                loanLockService.unlockAResource(accessToken, lockResourceResponse.getId(), loanId, tenant);
            }
        }
    }


    public List<JsonNode> fetchConditions(
            String loanId,
            boolean enhanced,
            String authHeader) {

        List<JsonNode> rawConditions = new ArrayList<>();

        if (enhanced) {
            rawConditions.add(fetch(loanId, ENHANCED, authHeader));
        } else {
            rawConditions.add(fetch(loanId, UNDERWRITING, authHeader));
            rawConditions.add(fetch(loanId, PRELIMINARY, authHeader));
            rawConditions.add(fetch(loanId, POSTCLOSING, authHeader));
        }

        return  rawConditions;
    }

    private JsonNode fetch(String loanId, ConditionType type, String authHeader) {

        JsonNode response = fetchConditions(loanId, type.getValue(), authHeader);

        if (response == null || response.isEmpty()) {
            log.info("No conditions returned for loanId={} type={}", loanId, type);
            return JsonNodeFactory.instance.arrayNode();
        }

        return response;
    }

    public List<ConditionDTO> mapAndNormalize(
            List<JsonNode> nodes,
            String loanId,
            boolean enhanced,
            String authHeader
    ) {

        List<ConditionDTO> result = new ArrayList<>();
        int order = 0;

        for (JsonNode array : nodes) {

            if (array == null || !array.isArray()) {
                continue;
            }

            for (JsonNode n : array) {

                String status = n.path("status").asText("").trim();

                boolean isClearedOrWaived =
                        status.equalsIgnoreCase("Cleared")
                                || status.equalsIgnoreCase("Waived");


                if (isClearedOrWaived) {
                    continue;
                }

                ConditionDTO c = new ConditionDTO();

                c.setId(n.path("id").asText());
                c.setTitle(n.path("title").asText(""));

                c.setDescription(
                        n.path("internalDescription").asText(
                                n.path("description").asText("")
                        )
                );

                c.setCategory(n.path("category").asText(""));
                c.setPriorTo(n.path("priorTo").asText(""));
                c.setConditionType(n.path("conditionType").asText(""));

                c.setStatus(status);

                String statusDateText = n.path("statusDate").asText("");
                if (StringUtils.hasText(statusDateText)) {
                    c.setStatusDate(
                            LocalDate.parse(statusDateText.substring(0, 10))
                    );
                }

                String comment = null;

                if (n.has("comments") && n.get("comments").isArray()) {
                    comment =
                            StreamSupport.stream(n.get("comments").spliterator(), false)
                                    .map(cn -> cn.path("comments").asText(""))
                                    .filter(StringUtils::hasText)
                                    .collect(Collectors.joining("\n\n"));
                }

                if (enhanced && !StringUtils.hasText(comment)) {
                    int commentsCount = n.path("commentsCount").asInt(0);
                    if (commentsCount > 0) {
                        comment = fetchConditionComments(
                                loanId,
                                c.getId(),
                                authHeader
                        );
                    }
                }

                c.setComment(comment);

                c.setOpen(true);

                c.setOrder(order++);

                result.add(c);
            }
        }

        return result;
    }

    private String fetchConditionComments(
            String loanId,
            String conditionId,
            String authHeader
    ) {
        String url =
                encompassBaseUrl + "/encompass/v3/loans/" + loanId + "/conditions/" + conditionId + "/comments";

        try {
            String response = WebClient.builder().build()
                    .get()
                    .uri(url)
                    .headers(headers -> headers.setBearerAuth(authHeader))
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                return null;
            }

            JsonNode commentsArray = objectMapper.readTree(response);

            return StreamSupport.stream(commentsArray.spliterator(), false)
                    .map(n -> n.path("comments").asText())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n\n"));

        } catch (Exception e) {
            log.error(
                    "Failed to fetch comments for loanId={} conditionId={}",
                    loanId,
                    conditionId,
                    e
            );
            return null;
        }
    }

    public long countClearedOrWaivedConditions(List<JsonNode> nodes) {

        long count = 0;

        for (JsonNode array : nodes) {

            if (array == null || !array.isArray()) {
                continue;
            }

            for (JsonNode n : array) {

                String status = n.path("status").asText("").trim();

                if ("Cleared".equalsIgnoreCase(status)
                        || "Waived".equalsIgnoreCase(status)) {
                    count++;
                }
            }
        }

        return count;
    }

}
