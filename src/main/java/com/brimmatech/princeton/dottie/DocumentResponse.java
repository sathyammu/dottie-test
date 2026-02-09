package com.brimmatech.princeton.dottie;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DocumentResponse {
    private String documentId;
    private String documentName;
    private List<AttachmentResponse> attachments;
}