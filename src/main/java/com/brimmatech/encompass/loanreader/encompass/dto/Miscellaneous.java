package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Miscellaneous{
	private String complianceVersionCd3X1505;
	private String savedLogVersion;
	private boolean tpoConnectStatusUpdated;
	private int loanFileSequenceNumber;
	private boolean newBuyDownEnabled;
	private String contactName;
	private String contactPhone;
}