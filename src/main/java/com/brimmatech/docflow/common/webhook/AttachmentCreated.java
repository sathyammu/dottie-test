package com.brimmatech.docflow.common.webhook;

import lombok.Data;

@Data
public class AttachmentCreated {
    private String id;
    private String title;
    private String documentTitle;
}
