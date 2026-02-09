package com.brimmatech.encompass.documentcreator.encompassdocuments;

import lombok.Data;

@Data
public class CommentDetails {
	private String comments;
	private String addedDate;
	private boolean isExternal;
	private String id;
}