package com.brimmatech.docflow.common.webhook;

import lombok.Data;

import java.util.List;

@Data
public class Event{
	private List<UpdateMilestonesItem> updateMilestones;
	private List<UpdateMilestonesItem> finishMilestones;
	private List<AssignAttachmentsToDocument> assignAttachmentsToDocument;
	private List<AttachmentCreated> attachmentCreated;
    private String previousLoanFolder;
    private String newLoanFolder;
}