package com.brimmatech.encompass.attachments;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentsObject {
    private String id;
    private String entityType;
    private String entityUri;
    private String authorizationHeader;
    private String contentType;
    private long fileSize;
    private int pageCount;
}
