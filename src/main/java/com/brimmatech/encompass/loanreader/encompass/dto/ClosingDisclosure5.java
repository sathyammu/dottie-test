package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClosingDisclosure5{
	private String signatureType;
	private Object totalPayments;
	private String settlementAgentName;
	private String settlementAgentAddress;
	private String settlementAgentCity;
	private String settlementAgentState;
	private String settlementAgentZip;
}