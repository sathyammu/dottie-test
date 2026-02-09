package com.brimmatech.docflow.approvaldisclosure;

import com.brimmatech.docflow.common.beanhelper.BeanHelper;
import com.brimmatech.docflow.common.webhook.WebhookDto;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.brimmatech.docflow.v2.models.ChangeLedgerEntity;
import com.brimmatech.docflow.v2.repository.ChangeLedgerRepository;
import com.brimmatech.docflow.v2.services.EncompassUpdater;
import com.brimmatech.encompass.loanreader.LoanReader;
import com.brimmatech.encompass.loanreader.fieldReader.FieldReaderResponse;
import com.brimmatech.encompass.loanreader.pipeline.Filter;
import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import com.brimmatech.encompass.loanreader.pipeline.PipelineRequest;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import com.brimmatech.encompass.tokengenerator.TokenService;
import com.brimmatech.mcp.LoanContextProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApprovalDisclosureService {

    private final BeanHelper beanHelper;
    private final TenantEntityRepository tenantEntityRepository;
    private final TokenService tokenService;
    private final LoanContextProvider loanContextProvider;
    private final ApprovalDisclosureRepository approvalDisclosureRepository;
    private final ObjectMapper objectMapper;
    private final ChangeLedgerRepository changeLedgerRepository;
    private final LoanReader loanReader;

    public void saveApprovalDisclosureLoans(String tenantId, WebhookDto webhookDto, TenantSettingsMeta.TaskBusinessFlowName flowName) {

        TenantEntity tenant = tenantEntityRepository.findById(Long.parseLong(tenantId))
                .orElseThrow(() -> new DocflowDataException(
                        "Tenant not found for provide tenant_id" + tenantId));
        ChangeLedgerEntity changeLedgerEntity = new ChangeLedgerEntity();

        try {
            changeLedgerEntity.setTenant(tenant);
            changeLedgerEntity.setDocQualifier(objectMapper.valueToTree(webhookDto));
            changeLedgerEntity.setFlowName(flowName);
            changeLedgerRepository.saveAndFlush(changeLedgerEntity);
        } catch (Exception e) {
            log.error("Exception while trying to persist in change ledger :{}", e.getMessage());
        }

        LoanReader loanReader = beanHelper.getLoanReaderBean(tenant.getSor().getSystemOfRecordName() + "-loanreader");

        TokenResponse tokenResponse = loanContextProvider.getToken(Long.parseLong(tenantId));

        String loanId = webhookDto.getMeta().getResourceId();

        PipelineRequest pipelineRequest = new PipelineRequest();

        Filter filter = new Filter();
        filter.setCanonicalName("Fields.GUID");
        filter.setValue("{" + webhookDto.getMeta().getResourceId() + "}");
        filter.setMatchType("exact");

        pipelineRequest.setFilter(filter);
        pipelineRequest.setFields(List.of( "Fields.2626",
                "Fields.2301",
                "Fields.LE1.X33",
                "Fields.364",
                "Fields.CX.COC.STATE.COM.FLAG",
                "Loan.LoanFolder"));

        // Fetch eligible loans
        PipelinePaginationResponse[] allLoans =
                loanReader.fetchApprovalLoan(pipelineRequest, tokenResponse.getAccessToken());

        if (allLoans == null || allLoans.length == 0) {
            log.warn("No eligible loans found for loanId {}", loanId);
            return;
        }

        PipelinePaginationResponse loan = allLoans[0];
        Map<String, String> fields = loan.getFields();

        // extract values
        String val2626 = fields.get("Fields.2626");
        String loanNumber = fields.get("Fields.364");
        String stateFlag = fields.get("Fields.CX.COC.STATE.COM.FLAG");

        LocalDate val2301 = null;
        LocalDate valLE1X33 = null;

        if (StringUtils.hasText(fields.get("Fields.2301"))) {
            val2301 = EncompassUpdater.parseDate(fields.get("Fields.2301")).orElse(null);
        }
        if (StringUtils.hasText(fields.get("Fields.LE1.X33"))) {
            valLE1X33 = EncompassUpdater.parseDate(fields.get("Fields.LE1.X33")).orElse(null);
        }

        // New: 2301 must not be earlier than yesterday
        LocalDate yesterday = LocalDate.now().minusDays(1);
        boolean is2301NotEarlierThanYesterday = val2301 != null && !val2301.isBefore(yesterday);

        // business checks
        boolean isRetail = val2626 != null && val2626.contains("Retail");
        boolean isLE1X33LessThan2301 =
                valLE1X33 != null && val2301 != null && valLE1X33.isBefore(val2301);

        List<ApprovalDisclosureEntity> approvalDisclosureEntities = approvalDisclosureRepository.findByLoanNumberAndStatusIgnoreCaseAndFlowName(loanNumber, "NEW", flowName.toString());

        if(approvalDisclosureEntities.isEmpty()) {

            if (isRetail && isLE1X33LessThan2301 && is2301NotEarlierThanYesterday
                    && StringUtils.hasText(loanNumber) && !StringUtils.hasText(stateFlag)) {
                ApprovalDisclosureEntity approvalDisclosureEntity = new ApprovalDisclosureEntity();
                approvalDisclosureEntity.setTenant(tenant);
                approvalDisclosureEntity.setLoanNumber(loanNumber);
                approvalDisclosureEntity.setStatus("NEW");
                approvalDisclosureEntity.setLoanFolder(fields.get("Loan.LoanFolder"));
                approvalDisclosureEntity.setFlowName(flowName.toString());
                approvalDisclosureRepository.saveAndFlush(approvalDisclosureEntity);
                changeLedgerEntity.setLogs("Approval Condition Success" + loanNumber + isRetail + isLE1X33LessThan2301 + is2301NotEarlierThanYesterday + stateFlag);
            } else {
                changeLedgerEntity.setLogs("Approval Condition Failed" + loanNumber + isRetail + isLE1X33LessThan2301 + is2301NotEarlierThanYesterday + stateFlag);
            }
        }
        changeLedgerRepository.save(changeLedgerEntity);
    }
    public String saveApprovalDisclosureLoansModel(String tenantId, WebhookDto webhookDto, TenantSettingsMeta.TaskBusinessFlowName flowName) {
        try {

            String loanGuid = null;

            if (webhookDto.getMeta() != null && webhookDto.getMeta().getResourceId() != null) {
                loanGuid = webhookDto.getMeta().getResourceId();
            }
            Optional<TenantEntity> tenant = tenantEntityRepository.findById(Long.valueOf(tenantId));
            String accessToken = loanContextProvider.getToken(Long.parseLong(tenantId)).getAccessToken();

            List<FieldReaderResponse> loanResponse  =  loanReader.fetchLoanDetails( loanGuid,List.of("364","LoanFolder"),accessToken);
            Map<String, String> fields = loanResponse.stream().collect(Collectors.toMap(FieldReaderResponse::getFieldId, v -> v.getValue().toString()));
            String loanNumber  = fields.get("364");
            String loanFolder = fields.get("LoanFolder");
            log.info("Field value {}",fields);
            if (loanNumber != null) {
                log.info("Disclosure field event detected for loan: {}", loanNumber);

                ChangeLedgerEntity change = new ChangeLedgerEntity();
                change.setTenant(tenant.get());
                change.setLoanNumber(loanNumber);
                change.setEventType(TenantSettingsMeta.TaskTriggeringEventType.DISCLOSURE_FIELD_CHANGE);
                change.setFlowName(flowName);
                change.setLogs("Event detected for Fields.CX.DISCLOSURE.YN");
                changeLedgerRepository.saveAndFlush(change);


                ApprovalDisclosureEntity approvalDisclosureEntity = new ApprovalDisclosureEntity();
                approvalDisclosureEntity.setTenant(tenant.get());
                approvalDisclosureEntity.setLoanNumber(loanNumber);
                approvalDisclosureEntity.setStatus("NEW");
                approvalDisclosureEntity.setLoanFolder(loanFolder);
                approvalDisclosureEntity.setFlowName(flowName.toString());
                approvalDisclosureRepository.saveAndFlush(approvalDisclosureEntity);

                return loanNumber;
            } else {
                log.warn("Field event detected but no loan number found in webhook");
                return "Loan number not found in webhook payload";
            }

        } catch (Exception e) {
            log.error("Error processing disclosure field event: {}", e.getMessage());
            return "Error processing webhook: " + e.getMessage();
        }
    }

    public void saveCOCDisclosureLoans(String tenantId, WebhookDto webhookDto, TenantSettingsMeta.TaskBusinessFlowName flowName) {

        TenantEntity tenant = tenantEntityRepository.findById(Long.parseLong(tenantId))
                .orElseThrow(() -> new DocflowDataException(
                        "Tenant not found for provided tenant_id: " + tenantId));

        ChangeLedgerEntity changeLedgerEntity = new ChangeLedgerEntity();
        try {
            changeLedgerEntity.setFlowName(flowName);
            changeLedgerEntity.setTenant(tenant);
            changeLedgerEntity.setDocQualifier(objectMapper.valueToTree(webhookDto));
            changeLedgerRepository.saveAndFlush(changeLedgerEntity);
        } catch (Exception e) {
            log.error("Exception while trying to persist in change ledger: {}", e.getMessage());
        }

        LoanReader loanReader = beanHelper.getLoanReaderBean(tenant.getSor().getSystemOfRecordName() + "-loanreader");
        TokenResponse tokenResponse = loanContextProvider.getToken(Long.parseLong(tenantId));

        String loanId = webhookDto.getMeta().getResourceId();

        PipelineRequest pipelineRequest = new PipelineRequest();

        Filter filter = new Filter();
        filter.setCanonicalName("Fields.GUID");
        filter.setValue("{" + webhookDto.getMeta().getResourceId() + "}");
        filter.setMatchType("exact");

        pipelineRequest.setFilter(filter);
        pipelineRequest.setFields(List.of( "Fields.CX.DISCLOSURE.YN",   // must be > 0
                "Fields.3152",               // not empty
                "Fields.LE1.X33",            // not today
                "Fields.3167",               // not empty
                "Fields.3977",               // must be empty
                "Fields.CX.CD.ELIGIBLE",     // not Yes
                "Fields.364",                // loan number
                "Loan.LoanFolder" ));

        // Fetch eligible loans
        PipelinePaginationResponse[] allLoans =
                loanReader.fetchApprovalLoan(pipelineRequest, tokenResponse.getAccessToken());

        if (allLoans == null || allLoans.length == 0) {
            log.warn("No eligible loans found for loanId {}", loanId);
            return;
        }

        PipelinePaginationResponse loan = allLoans[0];
        Map<String, String> fields = loan.getFields();

        // Extract values
        String loanNumber = fields.get("Fields.364");
        String loanFolder = fields.get("Loan.LoanFolder");

        // Parse values
        LocalDate valLE1X33 = null;
        if (StringUtils.hasText(fields.get("Fields.LE1.X33"))) {
            valLE1X33 = EncompassUpdater.parseDate(fields.get("Fields.LE1.X33")).orElse(null);
        }

        // Field-based conditions
        boolean disclosureYNValid = false;
        try {
            String disclosureYNValue = fields.get("Fields.CX.DISCLOSURE.YN");

            if (StringUtils.hasText(disclosureYNValue)) {
                disclosureYNValid = new BigDecimal(disclosureYNValue.trim())
                        .compareTo(BigDecimal.ZERO) > 0;
            }
        } catch (NumberFormatException ignored) {}

        boolean field3152NotEmpty = StringUtils.hasText(fields.get("Fields.3152"));
        boolean field3167NotEmpty = StringUtils.hasText(fields.get("Fields.3167"));
        boolean field3977Empty = !StringUtils.hasText(fields.get("Fields.3977"));
        boolean cdEligibleNotYes = !"Yes".equalsIgnoreCase(fields.get("Fields.CX.CD.ELIGIBLE"));

        boolean le1x33NotToday = valLE1X33 != null && !valLE1X33.isEqual(LocalDate.now());

        List<ApprovalDisclosureEntity> approvalDisclosureEntities = approvalDisclosureRepository.findByLoanNumberAndStatusIgnoreCaseAndFlowName(loanNumber, "NEW", flowName.toString());

        if(approvalDisclosureEntities.isEmpty()) {
            if (disclosureYNValid
                    && field3152NotEmpty
                    && field3167NotEmpty
                    && field3977Empty
                    && cdEligibleNotYes
                    && le1x33NotToday
                    && StringUtils.hasText(loanNumber)) {

                ApprovalDisclosureEntity approvalDisclosureEntity = new ApprovalDisclosureEntity();
                approvalDisclosureEntity.setTenant(tenant);
                approvalDisclosureEntity.setLoanNumber(loanNumber);
                approvalDisclosureEntity.setStatus("NEW");
                approvalDisclosureEntity.setLoanFolder(loanFolder);
                approvalDisclosureEntity.setFlowName(flowName.toString());

                approvalDisclosureRepository.saveAndFlush(approvalDisclosureEntity);
                log.info("Saved NEW approval disclosure for loanNumber {}", loanNumber);
                changeLedgerEntity.setLogs("COC Condition Success" + loanNumber + disclosureYNValid + field3152NotEmpty + field3167NotEmpty + field3977Empty + cdEligibleNotYes + le1x33NotToday);

            } else {
                log.info("Loan {} did not meet new disclosure criteria", loanNumber);
                changeLedgerEntity.setLogs("COC Condition Failed" + loanNumber + disclosureYNValid + field3152NotEmpty + field3167NotEmpty + field3977Empty + cdEligibleNotYes + le1x33NotToday);
            }

        }
        changeLedgerRepository.save(changeLedgerEntity);
    }

    public List<String> getNewDisclosuresByTenant(Long tenantId, List<String> loanFolders, TenantSettingsMeta.TaskBusinessFlowName flowName) {

        List<ApprovalDisclosureEntity> approvalDisclosureEntities = approvalDisclosureRepository.findByTenant_IdAndStatusIgnoreCaseAndLoanFolderInIgnoreCaseAndFlowName(tenantId.longValue(),
                "NEW", loanFolders, flowName.toString());

        return approvalDisclosureEntities
                .stream()
                .map(ApprovalDisclosureEntity::getLoanNumber)
                .toList();
    }

    public boolean updateStatusToCompleted(String loanNumber, String status) {
        List<ApprovalDisclosureEntity> entities = approvalDisclosureRepository.findByLoanNumber(loanNumber);

        if (entities == null || entities.isEmpty()) {
            log.warn("No matching records found for loanNumber={}", loanNumber);
            return false;
        }

        for (ApprovalDisclosureEntity entity : entities) {
            entity.setStatus(status);
        }

        approvalDisclosureRepository.saveAllAndFlush(entities);
        log.info("Updated status to {} for {} records (loan={})", status, entities.size(), loanNumber);

        return true;
    }
}
