package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

@Data
public class CreatedBy{
    private String entityName;
    private String entityType;
    private String entityId;
}