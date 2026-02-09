package com.brimmatech.encompass.documentupload;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import org.springframework.beans.factory.annotation.Value;


@lombok.extern.slf4j.Slf4j
public abstract class BaseDocumentUploadService implements ILOSDocumentUploadService {

    @Value("${task.uploadSavePath}")
    private String uploadPath;

    @Override
    public FileUploadDetail uploadDocumentToLOS(UploadAttachment uploadAttachment,
                                                String loanIdentifier,
                                                String accessToken,
                                                TenantEntity tenant) {
        return null;
    }

    @Override
    public FileUploadDetail uploadAttachmentToLOS(UploadAttachment uploadAttachment,
                                                  String loanIdentifier,
                                                  String accessToken,
                                                  TenantEntity tenant) {
        return null;
    }

}
