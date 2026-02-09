package com.brimmatech.docflow.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TenantsResponse {
    @JsonProperty("contents")
    private List<Tenant> contents;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_records")
    private int totalRecords;
}
