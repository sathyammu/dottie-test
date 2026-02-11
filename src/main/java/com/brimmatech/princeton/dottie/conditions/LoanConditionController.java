package com.brimmatech.princeton.dottie.conditions;

import com.brimmatech.encompass.attachments.dto.AttachmentUpdate;
import com.brimmatech.docflow.v2.dto.LoanUpdateDTO;
import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.brimmatech.encompass.conditions.dto.ConditionStatusRequest;
import com.brimmatech.encompass.documentcreator.DocumentService;
import com.brimmatech.encompass.documentupload.FileUploadDetail;
import com.brimmatech.encompass.documentupload.UploadAttachment;
import com.brimmatech.encompass.notes.NotesService;
import com.brimmatech.princeton.dottie.ConditionDocumentRequest;
import com.brimmatech.princeton.dottie.ConditionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class LoanConditionController {

    private final ConditionService conditionService;
    private final ConditionalAutomateEmail emailService;
    private final DocumentService documentService;
    private final NotesService notesService;

    @PutMapping("/tenant/{tenantId}/loan/{loanId}/conditions/{conditionType}")
    public ResponseEntity<?> updateStatus(
            @RequestHeader("Authorization") String userAccessToken,
            @PathVariable long tenantId,
            @PathVariable String loanId,
            @PathVariable String conditionType,
            @RequestBody List<ConditionStatusRequest> request) {

        return ResponseEntity.ok(
                conditionService.loanConditionStatusChange(userAccessToken, tenantId, loanId, conditionType, request)
        );
    }

    @GetMapping("/tenant/{tenantId}/loan/{loanId}/conditions/{type}/documents")
    public ResponseEntity<List<ConditionResponse>> getConditionDocuments(
            @PathVariable long tenantId,
            @PathVariable String loanId,
            @PathVariable String type,
            @RequestHeader("Authorization") String userAccessToken) {

        return ResponseEntity.ok(
                conditionService.fetchConditionDocuments(
                        loanId,
                        type,
                        userAccessToken
                )
        );
    }

    @PatchMapping("/tenant/{tenantId}/loan/{loanId}/{conditionType}/condition/{conditionId}/documents/{action}")
    public ResponseEntity<Void> assignDocuments(
            @PathVariable long tenantId,
            @PathVariable String loanId,
            @PathVariable String conditionType,
            @PathVariable String conditionId,
            @PathVariable String action,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<ConditionDocumentRequest> documents) {

        conditionService.assignDocumentsToConditionAndLoanLock(tenantId,
                loanId, conditionType, conditionId, documents, authHeader, action);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/tenant/{tenantId}/loan/{loanId}/attachment")
    public List<EncompassAttachment> getAttachments(@PathVariable String loanId,
                                                    @PathVariable String tenantId,
                                                    @RequestBody List<EncompassAttachment> attachments,
                                                    @RequestHeader("Authorization") String userToken){

        return documentService.fetchAttachment(loanId, tenantId, attachments, userToken);
    }

    @PatchMapping("/loan/{loanId}/renameAttachment")
    public ResponseEntity<?> updateAttachmentDetails(@PathVariable String loanId,
                                                     @RequestBody AttachmentUpdate attachmentUpdate,
                                                     @RequestHeader("Authorization") String userToken){

        if (attachmentUpdate.getTitle() == null || attachmentUpdate.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Title cannot be empty");
        }
        conditionService.updateAttachment(loanId, attachmentUpdate, userToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/tenant/{tenantId}/loan/{loanId}/attachments/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadDetail> uploadSingleAttachment(
            @PathVariable String tenantId,
            @PathVariable String loanId,
            @RequestHeader("Authorization") String userToken,
            @RequestParam("metadata") String request,
            @RequestParam("file") MultipartFile file) throws IOException {

        UploadAttachment attachment = new ObjectMapper().readValue(request, UploadAttachment.class);
        FileUploadDetail fileUploadDetail = conditionService.uploadAttachment(userToken, tenantId, loanId, attachment, file);

        return ResponseEntity.ok(fileUploadDetail);
    }

    @PatchMapping("/tenant/{tenantId}/loan/{loanId}/attachment/{attachmentId}")
    public ResponseEntity<Void> removeAttachment(
            @PathVariable String tenantId,
            @PathVariable String loanId,
            @PathVariable String attachmentId,
            @RequestHeader("Authorization") String authHeader) {

        conditionService.removeAttachment(authHeader, tenantId, loanId,
                attachmentId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/tenant/{tenantId}/loan/{loanId}/submit")
    public ResponseEntity<Void> submitConditions(@PathVariable String loanId,
                                                 @PathVariable long tenantId,
                                                 @RequestBody List<LoanUpdateDTO> request,
                                                 @RequestHeader("Authorization") String userToken){

        notesService.loanNotesTransfer(userToken, tenantId, loanId, request);
        return ResponseEntity.ok().build();
    }

}

