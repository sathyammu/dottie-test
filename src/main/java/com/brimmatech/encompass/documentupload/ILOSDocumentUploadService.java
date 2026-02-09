package com.brimmatech.encompass.documentupload;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;

public interface ILOSDocumentUploadService {
    FileUploadDetail uploadDocumentToLOS(UploadAttachment uploadAttachment,
                                         String loanIdentifier,
                                         String accessToken,
                                         TenantEntity tenant);

    FileUploadDetail uploadAttachmentToLOS(UploadAttachment uploadAttachment,
                                           String loanIdentifier,
                                           String accessToken,
                                           TenantEntity tenant);

}
