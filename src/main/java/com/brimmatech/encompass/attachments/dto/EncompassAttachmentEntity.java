package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

import java.util.List;

@Data
public class EncompassAttachmentEntity{
	private String createdByName;
	private String dateCreated;
	private boolean isRemoved;
	private List<PagesItem> pages;
	private String createdBy;
	private int attachmentType;
	private int fileSize;
	private Document document;
	private String attachmentId;
	private String title;
	private boolean isActive;
}