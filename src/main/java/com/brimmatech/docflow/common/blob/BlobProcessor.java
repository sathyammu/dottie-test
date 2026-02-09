package com.brimmatech.docflow.common.blob;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.brimmatech.docflow.chat.models.ExtractionDto.BlobUrlDto;
import com.brimmatech.docflow.exception.DocFlowDataProcessingException;
import com.brimmatech.docflow.v2.services.AzureBlobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

@Service @Slf4j
@RequiredArgsConstructor
public class BlobProcessor {

    @Value("${app.blob.connection-string}") private String connectionString;

    @Value("${app.blob.container-name}") private String containerName;


    private InputStream getInputStream(BlobUrlDto blobSasUrl) throws IOException {
        URL url = new URL(blobSasUrl.url());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to download blob. HTTP error code: " + connection.getResponseCode());
        }

        return connection.getInputStream();
    }

    public BlobUrlDto generateBlobSasToken(String blobName, Supplier<BlobClient> customBlobStorageProvider) {

        BlobClient
                blobClient =
                Objects.isNull(customBlobStorageProvider) ? getBlobClient(blobName) : customBlobStorageProvider.get();

        BlobSasPermission permissions = new BlobSasPermission().setReadPermission(true);

        BlobServiceSasSignatureValues
                sasValues =
                new BlobServiceSasSignatureValues(OffsetDateTime.now().plusHours(1), permissions).setProtocol(
                        SasProtocol.HTTPS_ONLY);

        String sasToken = blobClient.generateSas(sasValues);

        BlobProperties properties = blobClient.getProperties();

        byte[] contentMd5 = properties.getContentMd5();

        return new BlobUrlDto(blobClient.getBlobUrl() + "?" + sasToken,
                contentMd5 != null ? Base64.getEncoder().encodeToString(properties.getContentMd5()) : "");
    }

    public BlobClient getBlobClient(String blobName) {
        return new BlobClientBuilder().connectionString(connectionString)
                .containerName(containerName)
                .blobName(blobName)
                .buildClient();
    }
    public void uploadAttachmentToStorage(BlobRequest file,
                                          byte[] md5Bytes,
                                          Supplier<BlobClient> customBlobClientProvider) {

        try {
            BlobClient
                    blobClient =
                    Objects.isNull(customBlobClientProvider) ?
                            getBlobClient(file.getFileName()) :
                            customBlobClientProvider.get();

            blobClient.upload(BinaryData.fromBytes(file.getDocumentData()), true);

            try {
                blobClient.setHttpHeaders(new BlobHttpHeaders().setContentMd5(md5Bytes));
                log.info("Headers set successfully for blob: {}", file.getFileName());
            } catch (BlobStorageException e) {
                if (e.getStatusCode() == 409 &&
                        "BlobImmutableDueToPolicy".equalsIgnoreCase(e.getErrorCode().toString())) {
                    log.warn("Blob is immutable — cannot set headers for file: {}", file.getFileName());
                }
            }

            log.info("File uploaded to Azure Storage successfully: {}", file.getFileName());

        } catch (Exception e) {
            log.error("Failed to upload file to Azure Storage: Container Name: {}:{}",
                    containerName,
                    connectionString,
                    e);
            throw new DocFlowDataProcessingException("Failed to upload file to Azure Storage");
        }
    }

    public byte[] getDocumentByteArray(String blobFolderName) {
        try {
            BlobUrlDto blobSasUrl = generateBlobSasToken(blobFolderName, null);
            return getBytes(blobSasUrl);
        } catch (Throwable e) {
            return new byte[]{};
        }
    }


    public byte @NotNull [] getBytes(BlobUrlDto blobSasUrl) {
        try {
            InputStream inputStream = getInputStream(blobSasUrl);
            return inputStream.readAllBytes();
        } catch (Exception exception) {
            throw new DocFlowDataProcessingException(exception.getMessage());
        }
    }
}
