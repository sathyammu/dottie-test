package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClosingDisclosure4{
	private boolean seasonalPayments;
	private boolean stepPayments;
	private String escrowedPropertyCostsBasis;
	private boolean ignoreArmAdj;
	private boolean interestOnlyPayments;
}