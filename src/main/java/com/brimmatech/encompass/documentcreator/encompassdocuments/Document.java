package com.brimmatech.encompass.documentcreator.encompassdocuments;

import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Document {
	private String title;
	private List<String> accessibleTo;
	private boolean isMarkedRemoved;
	private String createdDate;
	private boolean isProtected;
	private List<String> documentTypes;
	private String id;
	private String status;
	private int daysTillExpire;
	private String statusDate;
	private String description;
	private String expectedDate;
	private int daysDue;
	private String requestedFrom;
	private String documentStatus;
	private String requestedDate;
	private String reviewedDate;
	private String receivedDate;
	private String readyForUwDate;
	private String emnSignature;
	private String readyToShipDate;
	private List<EncompassAttachment> attachments;
}