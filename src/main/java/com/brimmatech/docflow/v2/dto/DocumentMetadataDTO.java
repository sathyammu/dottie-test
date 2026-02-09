package com.brimmatech.docflow.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataDTO {
    private String documentId;
    private String fileName;
    private String blobStorageUrl;
    private ZonedDateTime createdAt;
    private Long id;
}