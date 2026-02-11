package com.brimmatech.encompass.attachments;

import com.brimmatech.encompass.attachments.dto.AttachmentUpdate;
import com.brimmatech.encompass.attachments.dto.EncompassAttachmentV3Response;

import java.util.List;
import java.util.Map;

public interface AttachmentProcessor {

    <T> List<T> retrieveDocumentAttachment(String loanGuid, String documentId,String accessToken);

    EncompassAttachmentV3Response retrieveAttachmentEntityDetail(String loanGuid, String attachmentId, String accessToken);

    EncompassAttachmentDownloadEntity downloadAttachmentUrl(String loanGuid,String accessToken,List<String> attachmentIds);

    List<EncompassAttachmentV3Response> retrieveAllAttachments(String loanGuid, String accessToken);

    void removeDocumentAttachment(String loanGuid, String attachmentId,String accessToken);

    void updateAttachmentDetails(String loanGuid, AttachmentUpdate attachmentUpdate, String accessToken);

    List<EncompassAttachmentDownloadEntityBatch> downloadAttachmentUrlBatchWise(String loanGuid, String accessToken, Map<String, String> attachmentIdsWithRequestIdMap);

}
