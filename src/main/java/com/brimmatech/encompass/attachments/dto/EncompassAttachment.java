package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

@Data
public class EncompassAttachment {
	private String entityUri;
	private String entityType;
	private String entityName;
	private String entityId;
	private byte[] attachmentContent;
    private Boolean isActive;
}