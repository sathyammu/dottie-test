package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Property{
	private Object freCashOutAmount;
	private boolean fhaSecondaryResidenceIndicator;
	private String typeRecordingJurisdiction;
	private String streetAddress;
	private String city;
	private String state;
	private  String postalCode;
	private String loanPurposeType;
	private String linkedLoanNumber;
}