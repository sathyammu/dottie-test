package com.brimmatech.encompass.notes;

import com.brimmatech.docflow.common.queueprocessor.QueueProcessorService;
import com.brimmatech.docflow.common.webhook.WebhookDto;
import com.brimmatech.docflow.exception.DocFlowDataProcessingException;
import com.brimmatech.docflow.v2.dto.LoanUpdateDTO;
import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notes") @AllArgsConstructor
@Slf4j
public class NotesController {

    private NotesService notesService;
    private ObjectMapper objectMapper;
    private QueueProcessorService queueProcessorService;

    @PostMapping("webhook/flow/{flowName}/client/{tenantId}")
    public ResponseEntity<String> processWebhook(HttpServletRequest request, @PathVariable long tenantId, @PathVariable TenantSettingsMeta.TaskBusinessFlowName flowName) {

        WebhookDto webhookDataDto;
        try {
            webhookDataDto = queueProcessorService.extractWebhookData(request);

            if (webhookDataDto == null) {
                log.error("Not able to extract loan guid from the webhook event");
                throw new DocFlowDataProcessingException("Not able to extract event information from webhook event", HttpStatus.BAD_REQUEST.value());
            }
            if(flowName.equals(TenantSettingsMeta.TaskBusinessFlowName.dottie_loan_notes)){
                notesService.updateNotesFromEncompass(tenantId, flowName, webhookDataDto);
            }

        } catch (IOException exception) {
            log.error("Not able to extract loan guid from the webhook event", exception);
            throw new DocFlowDataProcessingException("Not able to extract event information from webhook event", HttpStatus.BAD_REQUEST.value(), exception);
        }

        return new ResponseEntity<String>("Successfully consumed the webhook message", HttpStatus.OK);
    }

    @PostMapping("/tenant/{tenantId}/loan/{loanGuid}")
    public JsonNode updateLoanNotes(@PathVariable String loanGuid,
                                   @PathVariable long tenantId,
                                   @RequestBody JsonNode input) {

        List<NotesRequest> updates = new ArrayList<>();

        ArrayNode array = input.isArray() ? (ArrayNode) input : objectMapper.createArrayNode().add(input);

        for (JsonNode node : array) {
            NotesRequest dto = new NotesRequest();
            dto.setId(node.get("id").asText());
            dto.setLatestNote(node.get("latestNote").asText());
            dto.setUserInitial(node.get("userInitial").asText());

            JsonNode valueNode = node.get("value");

            if (valueNode.isArray() || valueNode.isObject()) {
                dto.setValue(valueNode.toString());
            } else {
                dto.setValue(valueNode.asText());
            }
            updates.add(dto);
        }

        return notesService.checkLoanLockAndUpdateEncompass(loanGuid, tenantId, updates);
    }


    @GetMapping("/tenant/{tenantId}/loan/{loanGuid}")
    public ResponseEntity<JsonNode> getNotes(@PathVariable long tenantId,
                                             @PathVariable String loanGuid) {

        JsonNode notes = notesService.getNotes(tenantId, loanGuid);
        return notes.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(notes);
    }

    @PostMapping("/tenant/{tenantId}/sync-legacy-notes")
    public ResponseEntity<Map<String, Object>> syncLegacyNotes(@PathVariable long tenantId) {

        return ResponseEntity.ok(
                notesService.syncLegacyNotesBatch(tenantId)
        );
    }


}
