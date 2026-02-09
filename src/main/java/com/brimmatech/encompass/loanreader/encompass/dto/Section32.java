package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section32{
	private String loanQualifyAsHighCostMortgage;
	private String resultOfPointAndFees;
	private String resultOfSecurityYieldTest;
	private Object totalPointsAndFees;
	private Object maximumPercentageOfLoan;
}