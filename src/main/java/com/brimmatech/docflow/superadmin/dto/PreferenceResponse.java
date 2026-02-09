package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class PreferenceResponse {
    private Long id;
    private String loEmail;
    private JsonNode preferences;
}
