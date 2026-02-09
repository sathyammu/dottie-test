package com.brimmatech.encompass.attachments;

import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("download:api")
@RequiredArgsConstructor
@Slf4j
@Deprecated
/**
 * Use BatchAttachmentDownloader as that implementation handles deleted pages (they dont be downloaded,
 * whereas this strategy does download the deleted page.
 *
 * We are not able to fix this strategy  as this strategy uses old API and the BatchAttachmentDownloader uses a new API
 */
public class ApiAttachmentDownloader implements AttachmentDownloadStrategy{

    private final AttachmentDownloadService attachmentDownloadService;

    @Override
    public List<EncompassAttachment> processDownloadAttachment(List<EncompassAttachment> attachments,
                                                               List<EncompassAttachment> enrichedAttachments,
                                                               AttachmentProcessor attachmentProcessor, String loanGuid,
                                                               TokenResponse tokenResponse) {
        int batchSize = 10;
        for (int i = 0; i < attachments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, attachments.size());
            List<EncompassAttachment> batch = attachments.subList(i, end);
            List<String> batchAttachmentIds = batch.stream()
                    .map(EncompassAttachment::getEntityId)
                    .toList();

            EncompassAttachmentDownloadEntity attachmentDownloadUrls = attachmentProcessor
                    .downloadAttachmentUrl(loanGuid, tokenResponse.getAccessToken(), batchAttachmentIds);

            log.info("Fetched download URLs for batch [{}-{}]", i, end);

            for (EncompassAttachment attachment : batch) {
                Optional<AttachmentsItem> matchEntity = attachmentDownloadUrls.getAttachments().stream()
                        .filter(download -> download.getId().equals(attachment.getEntityId()))
                        .findFirst();

                matchEntity.ifPresent(attachmentsItem -> attachmentDownloadService.downloadAttachmentAsPdf(attachment,
                        attachmentsItem,
                        attachment::setAttachmentContent));

                enrichedAttachments.add(attachment);
            }
        }

        return enrichedAttachments;
    }
}
