package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

@Data
public class PagesItem{
	private int fileSize;
	private int rotation;
	private PageImage pageImage;
	private String originalKey;
	private ThumbnailImage thumbnailImage;
}