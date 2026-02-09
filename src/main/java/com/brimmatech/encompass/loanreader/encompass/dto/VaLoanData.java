package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaLoanData{
	private boolean includeSectionBOtherIndicator;
	private boolean includeSectionGIndicator;
	private Object cashDisbursedToBorrowerAmount;
	private boolean includeSectionAIndicator;
	private boolean includeSectionCIndicator;
	private Object cdNonShoppableLessFundingFee;
	private boolean includeSectionEIndicator;
	private boolean includeLenderCreditIndicator;
	private Object vaRecoupmentTotalClosingCosts;
	private boolean includeSectionHIndicator;
	private Object totalClosingCostLessGuaranteeFee;
	private boolean includeSectionFIndicator;
	private Object disbursementsLessPayoffAmount;
	private boolean includeSectionBVaFundingFeeIndicator;
}