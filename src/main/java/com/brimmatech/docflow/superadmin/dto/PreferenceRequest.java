package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Data
public class PreferenceRequest {
    private String loEmail;
    private JsonNode preferences;
}
