package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.databind.JsonNode;

public interface DocumentExtractionDataDto{
    Integer getId();
    String getDocumentType();
    String getLoanNumber();
    JsonNode getExtractedJsonData();
}
