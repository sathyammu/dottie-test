package com.brimmatech.docflow.v2.services;

import com.brimmatech.docflow.common.beanhelper.BeanHelper;
import com.brimmatech.docflow.common.blob.BlobProcessor;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.exception.DocFlowDataProcessingException;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.v2.dto.LoanUpdateDTO;
import com.brimmatech.docflow.v2.dto.PurchaseAdviceDTO;
import com.brimmatech.docflow.v2.dto.TaskPayload;
import com.brimmatech.docflow.v2.models.ChangeLedgerEntity;
import com.brimmatech.docflow.v2.models.TenantSettings;
import com.brimmatech.docflow.v2.repository.ChangeLedgerRepository;
import com.brimmatech.docflow.v2.repository.TenantSettingsRepository;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.encompass.attachments.EncompassAttachmentProcessor;
import com.brimmatech.encompass.conditions.ConditionType;
import com.brimmatech.encompass.conditions.dto.ConditionStatusRequest;
import com.brimmatech.encompass.documentupload.EncompassDocumentUploadService;
import com.brimmatech.encompass.documentupload.UploadAttachment;
import com.brimmatech.encompass.loanreader.LoanReader;
import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import com.brimmatech.encompass.loanupdater.ILoanUpdater;
import com.brimmatech.encompass.loanupdater.LOSLoanUpdateService;
import com.brimmatech.encompass.lockHandler.LoanLockHandler;
import com.brimmatech.encompass.lockHandler.LoanLockService;
import com.brimmatech.encompass.lockHandler.encompasslockhandler.LockResourceResponse;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import com.brimmatech.encompass.tokengenerator.TokenService;
import com.brimmatech.mcp.LoanContextProvider;
import com.brimmatech.princeton.dottie.AssignDocumentRequest;
import com.brimmatech.princeton.dottie.ConditionDocumentRequest;
import com.brimmatech.princeton.dottie.conditions.ConditionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j public class EncompassUpdater {

    public static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            // Basic date formats
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("d-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("d MMM yyyy"),
            DateTimeFormatter.ofPattern("MMMM d, yyyy"),
            DateTimeFormatter.ofPattern("MMMM d yyyy"),
            DateTimeFormatter.ofPattern("MMM d, yyyy"),
            DateTimeFormatter.ofPattern("MMM d yyyy"),

            // DateTime formats (12-hour with AM/PM)
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm a"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a"),
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a"),
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"),
            DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm:ss a"),

            // DateTime formats (24-hour)
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),

            // ISO and variants
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_DATE_TIME
    );
    public final LoanContextProvider loanContextProvider;
    public final LOSLoanUpdateService losLoanUpdateService;
    public final TenantEntityRepository tenantEntityRepository;
    final String
            BORROWER_SIGNED_DATE_JSON_PATH =
            "/documents/0/fields/intent_to_proceed.loan_info/valueObject/value/valueObject/borrower.signature" +
                    ".date/valueString";
    final String
            LOAN_ESTIMATE_DATE_JSON_PATH =
            "/documents/0/fields/intent_to_proceed.loan_info/valueObject/value/valueObject/loan_estimate" +
                    ".date_issued/valueString";

    private final EncompassAttachmentProcessor encompassAttachmentProcessor;
    private final EncompassDocumentUploadService encompassDocumentUploadService;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final TokenService tokenService;
    private final BeanHelper beanHelper;
    private final ObjectMapper objectMapper;
    private final ChangeLedgerRepository changeLedgerRepository;
    private final BlobProcessor blobProcessor;
    private final LoanLockService loanLockService;
    private final ConditionService conditionService;


    private static LocalDate getEarliestDate(List<LocalDate> dates) {
        return dates.stream().min(LocalDate::compareTo).orElse(null);
    }

    public static Optional<LocalDate> parseDate(String date) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(date, formatter));
            } catch (DateTimeParseException exception) {
                log.info("");
            }
        }
        return Optional.empty();
    }

    private static String normalize(Object value) {
        return value == null ? "" : value.toString().trim().replaceAll("\\s+", " ").toLowerCase();
    }

    public boolean updateLoan(TenantEntity tenant,
                              PurchaseAdviceDTO fieldsToBeUpdate,
                              String loanNumber) {

        TokenResponse tokenResponse = loanContextProvider.getToken(tenant.getId());

        LoanReader loanReader = beanHelper.getLoanReaderBean(tenant.getSor().getSystemOfRecordName() + "-loanreader");

        PipelinePaginationResponse[]
                pipelinePaginationResponse =
                loanReader.fetchLoanGuid(loanNumber, tokenResponse.getAccessToken());

        if (pipelinePaginationResponse.length == 0) {
            throw new DocFlowDataProcessingException("Loan number invalid");
        }

        String loanIdentifier = pipelinePaginationResponse[0].getLoanId();

        LoanLockHandler
                loanLockHandler =
                beanHelper.getLoanLockHandler(tenant.getSor().getSystemOfRecordName().concat("-lock-handler"));

        LockResourceResponse
                lockResponse =
                loanLockHandler.lockResource(tokenResponse.getAccessToken(), loanIdentifier);

        ILoanUpdater
                loanUpdater =
                beanHelper.getLoanUpdaterBean(tenant.getSor().getSystemOfRecordName().concat("-loan-updater"));

        boolean
                updateStatus =
                loanUpdater.updateLoanByFields(fieldsToBeUpdate, loanIdentifier, tokenResponse.getAccessToken());

        loanLockHandler.unlockResource(tokenResponse.getAccessToken(), lockResponse.getId(), loanIdentifier);
        return updateStatus;
    }


    public void updateNotes(CreationArgs.EncompassUpdateTaskInput input, long tenantId) {

        TokenResponse tokenResponse = loanContextProvider
                .getToken(tenantId);

        Map<String, List<Object>> notes = input.taskInput().notes();

        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Notes payload is empty or null.");
        }

        Map.Entry<String, List<Object>> entry = notes.entrySet().iterator().next();

        String loanId = entry.getKey();
        List<Object> rawList = entry.getValue();

        if (rawList == null) {
            throw new IllegalArgumentException("Notes list for loanId " + loanId + " is null.");
        }
        List<LoanUpdateDTO> updates = new ArrayList<>();

        for (Object item : rawList) {

            if (item instanceof List<?> nestedList) {
                for (Object nestedItem : nestedList) {
                    updates.add(objectMapper.convertValue(nestedItem, LoanUpdateDTO.class));
                }
            } else {
                updates.add(objectMapper.convertValue(item, LoanUpdateDTO.class));
            }
        }

        Map<String, List<LoanUpdateDTO>> updateRequest = Map.of(loanId, updates);

        losLoanUpdateService.updateLoanFields(updateRequest, tokenResponse.getAccessToken());
    }


    public String updateComments(CreationArgs.EncompassUpdateTaskInput input, long tenantId) {

        Map<String, List<Object>> notes = input.taskInput().notes();

        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Notes payload is empty or null.");
        }

        Map.Entry<String, List<Object>> entry = notes.entrySet().iterator().next();

        String loanId = entry.getKey();
        List<Object> rawList = entry.getValue();

        if (rawList == null || rawList.isEmpty()) {
            throw new IllegalArgumentException("Comments list for loanId " + loanId + " is empty.");
        }


        TaskPayload payload = objectMapper.convertValue(rawList.get(0), TaskPayload.class);


        TokenResponse tokenResponse = tokenService.generateUserToken(tenantId, payload.getUserId());


        String response = losLoanUpdateService.updateLoanFields(
                payload.getLoanId(),
                payload.getConditionId(),
                payload.getConditionType(),
                objectMapper.convertValue(payload.getComments(), JsonNode.class),
                tokenResponse.getAccessToken()
        );

        return "Comments updated successfully for loanId: " + payload.getLoanId() +
                ", serverResponse=" + response;
    }


    public void updateCustomFields(CreationArgs.EncompassUpdateTaskInput encompassUpdateTaskInput) {

        log.info("Fetching change ledger entity for id :{}", encompassUpdateTaskInput.taskInput().changeLedgerId());

        Optional<ChangeLedgerEntity> changeLedgerEntity = changeLedgerRepository.
                findById(encompassUpdateTaskInput.taskInput().changeLedgerId());

        if (changeLedgerEntity.isPresent()) {

            String loanNumber = changeLedgerEntity.get().getLoanNumber();

            TenantEntity tenant = changeLedgerEntity.get().getTenant();

            TenantSettings tenantSettings = tenantSettingsRepository
                    .findByTenantIdAndCategoryAndStrategy(tenant.getId(),
                            SettingsCategory.TASK_TASK_ROUTE.getName(),
                            SettingsCategory.TASK_TASK_ROUTE.getStrategy())
                    .orElseThrow(() -> new IllegalArgumentException("TenantSettings not found"));

            JsonNode metaNode = tenantSettings.getMeta();

            if (metaNode != null && metaNode.isArray()) {
                for (JsonNode node : metaNode) {
                    if ("document_classification".equalsIgnoreCase(node.path("eventType").asText())) {
                        JsonNode actionNode = node.path("action");
                        JsonNode customFieldsNode = actionNode.path("customeFields");

                        if (customFieldsNode.isArray()) {
                            for (JsonNode field : customFieldsNode) {
                                String fieldId = field.path("fieldId").asText();

                                PurchaseAdviceDTO purchaseAdviceDTO = new PurchaseAdviceDTO();

                                if ("cx.title.classified.date".equalsIgnoreCase(fieldId)) {

                                    Map<String, String> fieldMap = new HashMap<>();
                                    String
                                            currentTimestamp =
                                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                    fieldMap.put(fieldId, currentTimestamp);
                                    purchaseAdviceDTO.setFieldMap(fieldMap);
                                }
                                updateLoan(tenant, purchaseAdviceDTO, loanNumber);

                            }
                        }
                    }
                }
            }
        }
    }

    public void updateConditionStatus(CreationArgs.EncompassUpdateTaskInput input, long tenantId) {

        Map<String, List<Object>> notes = input.taskInput().notes();

        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("status payload is empty or null.");
        }

        Map.Entry<String, List<Object>> entry = notes.entrySet().iterator().next();

        String loanId = entry.getKey();
        List<Object> rawList = entry.getValue();

        if (rawList == null || rawList.isEmpty()) {
            throw new IllegalArgumentException("status list for loanId " + loanId + " is empty.");
        }

        TokenResponse tokenResponse = loanContextProvider
                .getToken(tenantId);

        List<List<Map<String, Object>>> tripleNested =
                objectMapper.convertValue(rawList, new TypeReference<>() {
                });

        List<ConditionStatusRequest> requests =
                tripleNested.stream()
                        .flatMap(List::stream)
                        .map(map -> objectMapper.convertValue(map, ConditionStatusRequest.class))
                        .collect(Collectors.toList());

        String conditionType = requests.stream()
                .map(ConditionStatusRequest::getConditionType)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Condition type missing in request"));

        ConditionType resolvedType = ConditionType.fromString(conditionType);

        boolean isEnhanced = resolvedType == ConditionType.ENHANCED;

        if (!isEnhanced) {
            conditionService.updateConditionStatus(
                    tokenResponse.getAccessToken(),
                    tenantId,
                    loanId,
                    conditionType,
                    requests
            );
        } else {
            conditionService.updateEnhancedConditionStatuses(
                    tokenResponse.getAccessToken(),
                    tenantId,
                    loanId,
                    conditionType,
                    requests
            );
        }
    }

    public void updateConditionDocs(CreationArgs.EncompassUpdateTaskInput input, long tenantId) {

        TokenResponse tokenResponse = loanContextProvider
                .getToken(tenantId);

        Map<String, List<Object>> notes = input.taskInput().notes();

        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Notes payload is empty or null.");
        }

        for (Map.Entry<String, List<Object>> entry : notes.entrySet()) {

            String loanId = entry.getKey();
            List<Object> conditionObjects = entry.getValue();

            if (conditionObjects == null || conditionObjects.isEmpty()) {
                log.warn("No condition entries found for loanId {}", loanId);
                continue;
            }

            for (Object obj : conditionObjects) {
                AssignDocumentRequest conditionNote =
                        objectMapper.convertValue(obj, AssignDocumentRequest.class);

                String conditionId = conditionNote.getConditionId();
                String conditionType = conditionNote.getConditionType();
                List<ConditionDocumentRequest> documents =
                        conditionNote.getConditionDocumentRequestList();

                if (conditionId == null || conditionType == null || documents == null || documents.isEmpty()) {
                    log.warn(
                            "Skipping invalid condition entry for loanId {}. conditionId={}, conditionType={}",
                            loanId, conditionId, conditionType
                    );
                    continue;
                }

                log.info(
                        "Assigning {} document(s) to condition {} [{}] for loan {}",
                        documents.size(), conditionId, conditionType, loanId
                );

                conditionService.addDocumentsToCondition(loanId, conditionType, conditionId, documents,
                        "Bearer " + tokenResponse.getAccessToken(), conditionNote.getAction());
            }
        }
    }

    public void uploadAttachment(
            CreationArgs.EncompassUpdateTaskInput input,
            long tenantId
    ) {

        TokenResponse tokenResponse = loanContextProvider
                .getToken(tenantId);

        TenantEntity tenant = tenantEntityRepository.findById(tenantId)
                .orElseThrow(() -> new DocflowDataException(
                        "Tenant not found for provided tenant_id " + tenantId));

        Map<String, List<Object>> notes = input.taskInput().notes();

        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Upload attachment payload is empty or null.");
        }

        Map.Entry<String, List<Object>> entry = notes.entrySet().iterator().next();

        String loanId = entry.getKey();
        List<Object> rawList = entry.getValue();

        if (rawList == null || rawList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Upload attachment payload for loanId " + loanId + " is null or empty."
            );
        }

        for (Object item : rawList) {
            UploadAttachment attachment =
                    objectMapper.convertValue(item, UploadAttachment.class);


            String filePath = attachment.getAttachmentTitleWithFileExtension();

            if (filePath == null || filePath.isBlank()) {
                throw new IllegalArgumentException(
                        "Attachment file path is missing for loanId " + loanId
                );
            }

            byte[] fileBytes = blobProcessor.getDocumentByteArray(filePath);

            attachment.setAttachmentByteArrayData(fileBytes);
            attachment.setAttachmentTitleWithFileExtension(attachment.getAttachmentTitle());

            LockResourceResponse lockResourceResponse = null;

            lockResourceResponse = loanLockService.lockResource(tokenResponse.getAccessToken(), loanId, tenant);

            log.trace("Uploading attachment [{}] to document [{}] for loan [{}]",
                    attachment.getAttachmentTitle(),
                    attachment.getDocumentTitle(),
                    loanId);

            encompassDocumentUploadService.uploadDocumentToLOS(attachment,
                    loanId,
                    tokenResponse.getAccessToken(),
                    tenant);

            if (lockResourceResponse != null) {
                log.info("Unlocking loan after upload. loanId={}", loanId);
                loanLockService.unlockAResource(tokenResponse.getAccessToken(),
                        lockResourceResponse.getId(),
                        loanId,
                        tenant);
            }
        }
    }

    public void removeAttachment(CreationArgs.EncompassUpdateTaskInput input, long tenantId) {

        TokenResponse tokenResponse = loanContextProvider
                .getToken(tenantId);

        TenantEntity tenant = tenantEntityRepository.findById(tenantId)
                .orElseThrow(() -> new DocflowDataException(
                        "Tenant not found for provided tenant_id " + tenantId));

        Map<String, List<Object>> notes = input.taskInput().notes();

        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Upload attachment payload is empty or null.");
        }

        Map.Entry<String, List<Object>> entry = notes.entrySet().iterator().next();

        String loanId = entry.getKey();
        List<Object> rawList = entry.getValue();

        if (rawList == null || rawList.isEmpty()) {
            throw new IllegalArgumentException(
                    "remove attachment payload for loanId " + loanId + " is null or empty."
            );
        }

        for (Object item : rawList) {
            UploadAttachment attachment =
                    objectMapper.convertValue(item, UploadAttachment.class);

            LockResourceResponse lockResourceResponse = null;

            lockResourceResponse = loanLockService.lockResource(tokenResponse.getAccessToken(), loanId, tenant);

            log.trace("Remove attachment [{}] to document [{}] for loan [{}]",
                    attachment.getAttachmentTitle(),
                    attachment.getDocumentTitle(),
                    loanId);

            encompassAttachmentProcessor.removeDocumentAttachment(loanId,
                    attachment.getAttachmentTitle(),
                    tokenResponse.getAccessToken());

            if (lockResourceResponse != null) {
                log.info("Unlocking loan after remove attachment. loanId={}", loanId);
                loanLockService.unlockAResource(tokenResponse.getAccessToken(),
                        lockResourceResponse.getId(),
                        loanId,
                        tenant);
            }
        }
    }

}

