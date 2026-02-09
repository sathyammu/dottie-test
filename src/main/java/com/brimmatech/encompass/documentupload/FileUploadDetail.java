package com.brimmatech.encompass.documentupload;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileUploadDetail {
    @JsonProperty("document_id")
    private String documentId;
    @JsonProperty("attachmentid")
    private String attachmentId;
}
