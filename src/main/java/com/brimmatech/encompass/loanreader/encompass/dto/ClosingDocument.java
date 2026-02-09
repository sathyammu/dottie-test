package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClosingDocument{
	private List<AdditionalStateDisclosuresItem> additionalStateDisclosures;
	private boolean refinanceRightOfRescissionExemptFlag;
	private List<RespaHudDetailsItem> respaHudDetails;
	private String closingProvider;
	private Object disbursementsToBorrower;
	private boolean syncInterestDateDisbursementDate;
	private String finalVestingDescription;
}