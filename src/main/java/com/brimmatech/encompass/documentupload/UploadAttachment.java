package com.brimmatech.encompass.documentupload;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UploadAttachment {

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("document_title")
    private String documentTitle;

    @JsonProperty("force_create_new_document")
    private boolean forceCreateNewDocument;

    @JsonProperty("attachment_title")
    private String attachmentTitle;

    @JsonProperty("attachment_byte_array_data")
    private byte[] attachmentByteArrayData;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("attachment_title_with_file_extension")
    private String attachmentTitleWithFileExtension;
}
