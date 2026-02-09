package com.brimmatech.encompass.attachments.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReassignFolderAttachmentRequest {
    @JsonIgnore
    private String loanId;
    @JsonIgnore
    private String documentId;

    private String entityId;

    @JsonProperty("isActive")
    private boolean isActive;

    private String entityType;

}
