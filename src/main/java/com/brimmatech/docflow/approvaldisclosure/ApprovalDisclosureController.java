package com.brimmatech.docflow.approvaldisclosure;

import com.brimmatech.docflow.common.queueprocessor.QueueProcessorService;
import com.brimmatech.docflow.common.webhook.WebhookDto;
import com.brimmatech.docflow.exception.DocFlowDataProcessingException;
import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.brimmatech.princeton.dottie.conditions.ConditionalAutomateEmail;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/automation")
@RequiredArgsConstructor
@Slf4j
public class ApprovalDisclosureController {

    private final QueueProcessorService queueProcessorService;
    private final ApprovalDisclosureService approvalDisclosureService;
    private final ConditionalAutomateEmail emailService;

    @PostMapping("webhook/flow/{flowName}/client/{tenant-id}")
    public ResponseEntity<String> processWebhook(HttpServletRequest request, @PathVariable("tenant-id") String tenantId, @PathVariable("flowName") String flowName) {

        WebhookDto webhookDataDto;
        try {
            webhookDataDto = queueProcessorService.extractWebhookData(request);

            if (webhookDataDto == null) {
                log.error("Not able to extract loan guid from the webhook event");
                throw new DocFlowDataProcessingException("Not able to extract event information from webhook event", HttpStatus.BAD_REQUEST.value());
            }
            if(flowName.equalsIgnoreCase("approval-disclosure")){
                approvalDisclosureService.saveApprovalDisclosureLoans(tenantId, webhookDataDto, TenantSettingsMeta.TaskBusinessFlowName.fromValue(flowName));
            }else if(flowName.equalsIgnoreCase("pre-closing-disclosure")){
                approvalDisclosureService.saveApprovalDisclosureLoansModel(tenantId, webhookDataDto, TenantSettingsMeta.TaskBusinessFlowName.fromValue(flowName));
            }else if(flowName.equalsIgnoreCase("coc-disclosure")) {
                approvalDisclosureService.saveCOCDisclosureLoans(tenantId, webhookDataDto, TenantSettingsMeta.TaskBusinessFlowName.fromValue(flowName));
            }
        } catch (IOException exception) {
            log.error("Not able to extract loan guid from the webhook event", exception);
            throw new DocFlowDataProcessingException("Not able to extract event information from webhook event", HttpStatus.BAD_REQUEST.value(), exception);
        }

        return new ResponseEntity<String>("Successfully consumed the webhook message", HttpStatus.OK);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<String>> getNewDisclosures(
            @PathVariable Long tenantId,
            @RequestParam(required = false) List<String> loanFolders,
            @RequestParam(required = false) TenantSettingsMeta.TaskBusinessFlowName flowName) {

        List<String> disclosures = approvalDisclosureService.getNewDisclosuresByTenant(tenantId, loanFolders, flowName);
        return ResponseEntity.ok(disclosures);
    }

    @PatchMapping("/{loanNumber}/status/{status}")
    public ResponseEntity<String> markAsCompleted(@PathVariable String loanNumber, @PathVariable String status) {
        boolean updated = approvalDisclosureService.updateStatusToCompleted(loanNumber, status);

        if (updated) {
            return ResponseEntity.ok("Status updated to COMPLETED for loan: " + loanNumber);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("webhook/dottie/flow/{flowName}/client/{tenant-id}")
    public ResponseEntity<String> processDottieWebhook(HttpServletRequest request, @PathVariable("tenant-id") long tenantId, @PathVariable("flowName") String flowName) {

        WebhookDto webhookDataDto;
        try {
            webhookDataDto = queueProcessorService.extractWebhookData(request);
            if (webhookDataDto == null) {
                log.error("Not able to extract loan guid from the webhook event");
                throw new DocFlowDataProcessingException("Not able to extract event information from webhook event", HttpStatus.BAD_REQUEST.value());
            }
            String loanId = webhookDataDto.getMeta().getResourceId();

            if(flowName.equalsIgnoreCase("file-review")){
                emailService.sendFileReviewCompleteEmail(loanId, tenantId, flowName);
            }else {
                emailService.sendConditionalStatusEmail(loanId, tenantId, flowName);
            }

        } catch (IOException exception) {
            log.error("Not able to extract loan guid from the webhook event", exception);
            throw new DocFlowDataProcessingException("Not able to extract event information from webhook event", HttpStatus.BAD_REQUEST.value(), exception);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ResponseEntity<String>("Successfully consumed the webhook message", HttpStatus.OK);
    }

    }
