package com.brimmatech.encompass.loanreader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmploymentRequest {
    private String id;
    private String addressCity;
    private String addressPostalCode;
    private String addressState;
    private String addressStreetLine1;
    private String employerName;
    private boolean currentEmploymentIndicator;
    private boolean militaryEmployer;
}
