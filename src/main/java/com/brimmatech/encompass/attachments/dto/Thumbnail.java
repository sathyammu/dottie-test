package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

@Data
public class Thumbnail{
	private Object horizontalResolution;
	private String zipKey;
	private String imageKey;
	private Object verticalResolution;
	private int width;
	private int height;
}