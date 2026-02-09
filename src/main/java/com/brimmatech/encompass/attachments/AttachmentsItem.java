package com.brimmatech.encompass.attachments;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentsItem{
	private List<PagesItem> pages;
	private List<String> originalUrls;
	private String id;
	private String url;
	private String authorizationHeader;
	private String contentType;
}