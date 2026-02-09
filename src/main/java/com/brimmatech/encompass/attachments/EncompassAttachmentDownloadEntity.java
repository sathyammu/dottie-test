package com.brimmatech.encompass.attachments;

import lombok.Data;

import java.util.List;

@Data
public class EncompassAttachmentDownloadEntity{
	private List<AttachmentsItem> attachments;
}