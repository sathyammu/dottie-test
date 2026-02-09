package com.brimmatech.encompass.attachments;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class AttachmentBatchExportResultDto {
    private String jobId;
    private String status;
    private AttachmentsObject file;
}
