package com.brimmatech.encompass.attachments;

import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface AttachmentDownloadStrategy {

    List<EncompassAttachment> processDownloadAttachment(List<EncompassAttachment> attachments,
                                                        List<EncompassAttachment> enrichedAttachments,
                                                        AttachmentProcessor attachmentProcessor, String loanGuid,
                                                        TokenResponse tokenResponse) throws JsonProcessingException;
}
