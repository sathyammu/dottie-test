package com.brimmatech.encompass.documentupload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignDocumentRequest {
    @JsonProperty("loan_identifier")
    private String loanIdentifier;
    @JsonProperty("document_id")
    private String documentId;
    @JsonProperty("attachment_id")
    private String attachmentId;
}
