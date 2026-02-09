package com.brimmatech.encompass.documentcreator;

import com.brimmatech.docflow.common.beanhelper.BeanHelper;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.v2.models.ChangeLedgerEntity;
import com.brimmatech.encompass.attachments.AttachmentProcessor;
import com.brimmatech.encompass.attachments.BatchAttachmentDownloader;
import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.brimmatech.encompass.documentcreator.encompassdocuments.Document;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final BeanHelper beanHelper;
    private final  BatchAttachmentDownloader batchAttachmentDownloader;
    private final TenantEntityRepository tenantEntityRepository;


    public List<EncompassAttachment> fetchAttachmentDetails(String loanGuid,
                                                            TenantEntity tenant,
                                                            ChangeLedgerEntity changeLedgerEntity,
                                                            TokenResponse tokenResponse,
                                                            List<Document> documents) {

        log.info("Fetched all documents for loanGuid :{}", loanGuid);

        AttachmentProcessor attachmentProcessor = beanHelper
                .getAttachmentProcessor(tenant.getSor().getSystemOfRecordName().concat("-attachment-processor"));

        return documents.stream().map(document ->
                        eliminateDuplicateAttachmentInDocuments(loanGuid,
                                document,
                                changeLedgerEntity,
                                attachmentProcessor,
                                tokenResponse))
                .flatMap(List::stream)
                .toList();
    }

    private List<EncompassAttachment> eliminateDuplicateAttachmentInDocuments(String loanGuid, Document document,
                                                                              ChangeLedgerEntity changeLedgerEntity,
                                                                              AttachmentProcessor attachmentProcessor,
                                                                              TokenResponse tokenResponse) {

        log.info("Fetching all attachments for set of documents that retrieved fot loanId :{}, docId: {} ",
                loanGuid,
                document.getId());

        List<EncompassAttachment> attachmentDetail = attachmentProcessor
                .retrieveDocumentAttachment(loanGuid, document.getId(), tokenResponse.getAccessToken());

        Map<String, List<EncompassAttachment>> groupedAttachments = attachmentDetail.stream()
                .collect(Collectors.groupingBy(EncompassAttachment::getEntityName));

        log.info("Filtering all duplicate attachments based on created date");

        return groupedAttachments.values().stream().flatMap(attachment -> (
                        !"signed_disclosure".equals(changeLedgerEntity.getFlowName())) ?
                        filterAttachmentBasedOnCreatedDate(loanGuid,
                                tokenResponse.getAccessToken(),
                                attachment,
                                attachmentProcessor).stream()
                        : attachment.stream())
                .toList();
    }

    private Optional<EncompassAttachment> filterAttachmentBasedOnCreatedDate(String loanGuid, String accessToken,
                                                                             List<EncompassAttachment> attachment,
                                                                             AttachmentProcessor attachmentProcessor) {
        return attachment.stream()
                .max(Comparator.comparing(attach ->
                        attachmentProcessor.retrieveAttachmentEntityDetail(loanGuid, attach.getEntityId(), accessToken)
                                .getCreatedDate()));
    }

    public List<EncompassAttachment> fetchAllAttachments(String loanGuid,
                                                         TenantEntity tenant,
                                                         List<Document> documents,
                                                         TokenResponse tokenResponse
                                                         ) {


        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        List<EncompassAttachment> attachments = documents.stream()
                .flatMap(document -> document.getAttachments() != null
                        ? document.getAttachments().stream()
                        : Stream.empty())
                .collect(Collectors.toList());

        log.info("Filtered attachments: {}",
                attachments.stream().collect(Collectors.toMap(EncompassAttachment::getEntityId,
                        EncompassAttachment::getEntityName)));

        AttachmentProcessor attachmentProcessor = beanHelper
                .getAttachmentProcessor(tenant.getSor().getSystemOfRecordName().concat("-attachment-processor"));

        List<EncompassAttachment> enrichedAttachments = new ArrayList<>();

        return batchAttachmentDownloader.processDownloadAttachment(attachments,enrichedAttachments,attachmentProcessor,loanGuid,tokenResponse);

    }


    public List<EncompassAttachment> fetchAttachment(String loanGuid,
                                                         String tenantId,
                                                         List<EncompassAttachment> attachments,
                                                         String userToken
    ) {

        TenantEntity tenant = tenantEntityRepository.findById(Long.valueOf(tenantId))
                .orElseThrow(() -> new DocflowDataException(
                        "Tenant not found for provide tenant_id" + tenantId));
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(userToken);

        AttachmentProcessor attachmentProcessor = beanHelper
                .getAttachmentProcessor(tenant.getSor().getSystemOfRecordName().concat("-attachment-processor"));

        List<EncompassAttachment> enrichedAttachments = new ArrayList<>();

        return batchAttachmentDownloader.processDownloadAttachment(attachments,enrichedAttachments,attachmentProcessor,loanGuid,tokenResponse);

    }


}
