package com.brimmatech.encompass.attachments;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncompassAttachmentDownloadEntityBatch {
    private String id;
    private String requestId;
    private String status;
    private AttachmentsObject object;
}
