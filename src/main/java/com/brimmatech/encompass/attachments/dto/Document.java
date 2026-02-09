package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

@Data
public class Document{
	private String entityUri;
	private String entityType;
	private String entityName;
	private String entityId;
}