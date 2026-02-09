package com.brimmatech.docflow.chat.models;

public sealed interface ExtractionDto {

    record BlobUrlDto(String url, String contentMd5) implements ExtractionDto {
    };

}
