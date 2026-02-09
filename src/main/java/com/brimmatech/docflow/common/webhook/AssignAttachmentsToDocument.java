package com.brimmatech.docflow.common.webhook;

import lombok.Data;

import java.util.List;

@Data
public class AssignAttachmentsToDocument {
    private String id;
    private String title;
    private List<String> attachmentIds;
}
