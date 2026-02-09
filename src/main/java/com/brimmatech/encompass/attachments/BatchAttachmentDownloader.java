package com.brimmatech.encompass.attachments;

import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component("download:batch")
@RequiredArgsConstructor
@Slf4j
public class BatchAttachmentDownloader implements AttachmentDownloadStrategy {

    private final AttachmentDownloadService attachmentDownloadService;
    private final EncompassAttachmentProcessor encompassAttachmentProcessor;

    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(8);

    @Override
    public List<EncompassAttachment> processDownloadAttachment(List<EncompassAttachment> attachments,
                                                               List<EncompassAttachment> enrichedAttachments,
                                                               AttachmentProcessor attachmentProcessor,
                                                               String loanGuid, TokenResponse tokenResponse) {

        int batchSize = 10;

        for (int i = 0; i < attachments.size(); i += batchSize) {

            int end = Math.min(i + batchSize, attachments.size());
            List<EncompassAttachment> batch = attachments.subList(i, end);

            AtomicInteger counter = new AtomicInteger(1);

            Map<String, String> batchAttachmentMap = batch.stream()
                    .collect(Collectors.toMap(
                            EncompassAttachment::getEntityId,
                            att -> "@exportReq".concat(String.valueOf(counter.getAndIncrement()))
                    ));



            List<EncompassAttachmentDownloadEntityBatch> downloadEntities =
                    attachmentProcessor.downloadAttachmentUrlBatchWise(
                            loanGuid, tokenResponse.getAccessToken(), batchAttachmentMap);

            log.info("Fetched download URLs for batch [{}-{}]", i, end);

            Map<String, EncompassAttachmentDownloadEntityBatch> entityMap =
                    downloadEntities.stream()
                            .collect(Collectors.toMap(
                                    EncompassAttachmentDownloadEntityBatch::getRequestId,
                                    e -> e
                            ));

            List<CompletableFuture<EncompassAttachment>> futures = new ArrayList<>();

            for (EncompassAttachment attachment : batch) {

                EncompassAttachmentDownloadEntityBatch entity = entityMap.get(batchAttachmentMap.get(attachment.getEntityId()));

                if (entity == null) {
                    log.error("No response from Encompass for attachment ID {}. Skipping...", attachment.getEntityId());
                    continue;
                }

                futures.add(
                        CompletableFuture.supplyAsync(
                                () -> downloadAttachment(attachment, entity, tokenResponse),
                                downloadExecutor
                        )
                );
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            enrichedAttachments.addAll(
                    futures.stream().map(CompletableFuture::join).toList()
            );
        }

        return enrichedAttachments;
    }

    private EncompassAttachment downloadAttachment(EncompassAttachment attachment,
                                                   EncompassAttachmentDownloadEntityBatch entity,
                                                   TokenResponse tokenResponse) {

        AttachmentBatchExportResultDto result;

        if ("Queued".equalsIgnoreCase(entity.getStatus())) {
            result = encompassAttachmentProcessor.pollExportJobStatus(
                    entity.getId(),
                    tokenResponse.getAccessToken(),
                    10,
                    Duration.ofSeconds(5)
            );
        } else {
            result = AttachmentBatchExportResultDto.builder()
                    .status(entity.getStatus())
                    .jobId(entity.getId())
                    .file(entity.getObject())
                    .build();
        }

        if (result.getFile() == null) {
            throw new IllegalStateException("Export completed but no file returned for job: " + result.getJobId());
        }

        AttachmentsItem item = new AttachmentsItem();
        item.setId(result.getJobId());
        item.setUrl(result.getFile().getEntityUri());
        item.setAuthorizationHeader(result.getFile().getAuthorizationHeader());
        item.setContentType(result.getFile().getContentType());

        attachmentDownloadService.downloadAttachmentAsPdf(
                attachment,
                item,
                attachment::setAttachmentContent
        );

        return attachment;
    }

}
