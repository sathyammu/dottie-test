package com.brimmatech.encompass.documentupload;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.exception.LoanLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service("encompass-document-upload")
@Slf4j
@RequiredArgsConstructor
public class EncompassDocumentUploadService extends BaseDocumentUploadService {


    @Value("${api.los-service.host}")
    private String losBaseUrl;
    @Value("${api.los-service.attachment-upload-url}")
    private String attachmentUploadUrl;
    @Value("${api.los-service.assign-source-attachment-url}")
    private String assignSourceAttachmentUrl;


    private WebClient webClient;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        final int size = 16 * 1024 * 1024;

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        webClient = WebClient.builder().baseUrl(losBaseUrl).exchangeStrategies(strategies).build();

    }

    @Override
    public FileUploadDetail uploadDocumentToLOS(UploadAttachment uploadAttachment,
                                                String loanIdentifier,
                                                String accessToken,
                                                TenantEntity tenant) {

        log.info("Upload starts for loanIdentifier :{} , {}", loanIdentifier, uploadAttachment.getDocumentTitle());

        return webClient.post()
                .uri(attachmentUploadUrl, tenant.getId(), loanIdentifier, tenant.getSor().getSystemOfRecordName())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(uploadAttachment).retrieve()
                .bodyToMono(FileUploadDetail.class)
                .block();
    }

    @Override
    public FileUploadDetail uploadAttachmentToLOS(UploadAttachment uploadAttachment,
                                                  String loanIdentifier,
                                                  String accessToken,
                                                  TenantEntity tenant) {

        log.info("Attachment Upload starts for loanIdentifier :{} , {}",
                loanIdentifier,
                uploadAttachment.getDocumentTitle());

        return webClient.post()
                .uri(attachmentUploadUrl, tenant.getId(), loanIdentifier, tenant.getSor().getSystemOfRecordName())
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(uploadAttachment).retrieve()
                .bodyToMono(FileUploadDetail.class)
                .onErrorMap(WebClientResponseException.Conflict.class,
                        ex -> new LoanLockException("Conflict while uploading document: " + ex.getMessage()))
                .block();
    }


}
