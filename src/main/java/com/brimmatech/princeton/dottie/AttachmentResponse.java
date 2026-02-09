package com.brimmatech.princeton.dottie;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttachmentResponse {
    private String attachmentId;
    private String fileName;
    private Long fileSize;
    private String createdDate;
    private String createdBy;
}