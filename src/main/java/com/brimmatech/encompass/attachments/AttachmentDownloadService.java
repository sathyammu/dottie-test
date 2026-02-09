package com.brimmatech.encompass.attachments;

import com.brimmatech.encompass.attachments.dto.EncompassAttachment;
import com.brimmatech.general.types.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentDownloadService {

    private final OkHttpClient okHttpClient;

    public Map.Entry< String, byte[]> downloadAttachmentByte(String baseurl, String authorization) {

        log.info("BaseUrl:{}", baseurl);

        Map.Entry<String, byte[]> documentByte = null;

        try {

            Request.Builder requestBuilder = new Request.Builder()
                    .url(baseurl)
                    .get()
                    .addHeader("Accept", "application/pdf");
            if (StringUtils.hasText(authorization)) {
                requestBuilder.addHeader("Authorization", authorization);
            }


            try (Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
                documentByte = getStringEntry(response);
            } catch (IOException e) {
                System.err.println("An error occurred while fetching PDF: " + e.getMessage());
                return null;
            }




        } catch (Exception exception) {
            log.info("Exception: {}", exception.getMessage());
            log.trace("Exception stack trace", exception);
        }

        return documentByte;
    }

    private static Map.Entry<String, byte[]> getStringEntry(Response response) throws IOException {
        if (response.isSuccessful() && response.body() != null) {
            String v = response.headers().get(HttpHeaders.CONTENT_TYPE);
            return Map.entry( Objects.isNull(v) ? "application/octet-stream" : v, response.body().bytes());
        } else {
            System.err.println("Failed to fetch PDF. Response code: " + response.code());
            return null;
        }
    }

    public void downloadAttachmentAsPdf(EncompassAttachment attachment,
                                        AttachmentsItem matchEntity,
                                        Consumer<byte[]> contentAcceptor) {

        Map.Entry< String, byte[]> attachmentByte = null;

        try {
            String url = Optional.ofNullable(matchEntity.getUrl())
                    .or(() -> Optional.ofNullable(matchEntity.getOriginalUrls())
                            .filter(list -> !list.isEmpty())
                            .map(list -> list.get(0)))
                    .orElse(null);

            String authHeader = (matchEntity.getUrl() != null)
                    ? matchEntity.getAuthorizationHeader()
                    : null;

            if (url != null) {
                attachmentByte = downloadAttachmentByte(url, authHeader);
                try (PDDocument mergedDocument = new PDDocument()) {
                    val result = convertImageToPdf(attachmentByte, mergedDocument);
                    if (result.left.contains("image")) {
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            mergedDocument.save(baos);
                            attachmentByte.setValue(baos.toByteArray());
                        }
                    }
                }
            } else if (matchEntity.getPages() != null && !matchEntity.getPages().isEmpty()) {

                try (PDDocument mergedDocument = new PDDocument()) {
                    for (var page : matchEntity.getPages()) {
                        Map.Entry<String, byte[]>
                                fileBytes =
                                downloadAttachmentByte(page.getUrl(),
                                        matchEntity.getAuthorizationHeader());
                        if (fileBytes == null) continue;
                        convertImageToPdf(fileBytes, mergedDocument);
                    }

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        mergedDocument.save(baos);
                        attachmentByte = Map.entry("dummy", baos.toByteArray());
                    }
                }
            } else {
                log.warn("No valid download URL or pages found for attachment ID: {}", attachment.getEntityId());
            }

            if (attachmentByte != null && attachmentByte.getValue().length > 0) {
                contentAcceptor.accept(attachmentByte.getValue());
            } else {
                log.warn("Attachment byte is null or empty for ID: {}, skipping upload", attachment.getEntityId());
            }

        } catch (Exception ex) {
            log.error("Error processing attachment ID {}: {}", attachment.getEntityId(), ex.getMessage(), ex);
        }
    }

    private Tuple<String, Path> convertImageToPdf(Map.Entry<String, byte[]> mimeAndContent, PDDocument mergedDocument) throws IOException {

        Tuple<String, Path> fileProbeResult = probeFileType(mimeAndContent);

        if (fileProbeResult.left != null && fileProbeResult.left.contains("image")) {
            PDImageXObject
                    image =
                    PDImageXObject.createFromFile(fileProbeResult.right.toAbsolutePath().toString(), mergedDocument);
            PDPage pdfPage = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            mergedDocument.addPage(pdfPage);

            try (PDPageContentStream contentStream = new PDPageContentStream(mergedDocument, pdfPage)) {
                contentStream.drawImage(image, 0, 0, image.getWidth(), image.getHeight());
            }
        }

        if (fileProbeResult.left != null && fileProbeResult.left.contains("application/pdf")) {
            try (PDDocument tempDoc = Loader.loadPDF(mimeAndContent.getValue())) {
                mergedDocument.addPage(tempDoc.getPage(0)); // Or loop through all pages
            }
        }

        Files.deleteIfExists(fileProbeResult.right);
        return fileProbeResult;
    }

    private Tuple<String, Path> probeFileType(Map.Entry<String, byte[]> contentAndMimeType) throws IOException {
        String extension;
        if (contentAndMimeType.getKey().contains("png")) {
            extension = ".png";
        } else if (contentAndMimeType.getKey().contains("jpeg") || contentAndMimeType.getKey().contains("jpg")) {
            extension = ".jpg";
        } else if (contentAndMimeType.getKey().contains("tiff")) {
            extension = ".tif";
        } else if (contentAndMimeType.getKey().contains("pdf")) {
            extension = ".pdf";
        } else {
            extension = ".tmp";
        }
        Path tempFile = Files.createTempFile("page_", extension);
        Files.write(tempFile, contentAndMimeType.getValue());
        String mimeType = Files.probeContentType(tempFile);
        if(Objects.isNull(mimeType)) {
            mimeType = contentAndMimeType.getKey();
        }
        return new Tuple<>(mimeType, tempFile);
    }

}
