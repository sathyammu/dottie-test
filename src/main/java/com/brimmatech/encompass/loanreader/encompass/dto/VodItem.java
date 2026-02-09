package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VodItem {
    private int itemNumber;
    private String type;
    private String accountIdentifier;
    private Double urla2020CashOrMarketValueAmount;
    private String depositoryAccountGuid;
    private String depositoryAccountName;
}
