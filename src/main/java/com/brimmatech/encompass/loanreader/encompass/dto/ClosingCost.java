package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClosingCost{
	private Gfe2010Page gfe2010Page;
	private ClosingDisclosure1 closingDisclosure1;
	private String impoundType3;
	private LoanEstimate2 loanEstimate2;
	private LoanEstimate3 loanEstimate3;
	private Gfe2010Section gfe2010Section;
	private LoanEstimate1 loanEstimate1;
	private Gfe2010 gfe2010;
	private ClosingDisclosure3 closingDisclosure3;
	private ClosingDisclosure2 closingDisclosure2;
	private ClosingDisclosure5 closingDisclosure5;
	private ClosingDisclosure4 closingDisclosure4;
}