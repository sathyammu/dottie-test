package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanProductData{
	private boolean prepaymentPenaltyIndicator;
	private boolean helocEscrowAccountIndicator;
	private boolean helocTaInstallmentLoanDiscountedAprIndicator;
	private boolean helocRepaymentBasis;
	private String rateAdjustmentSubsequentCapPercentUi;
	private boolean helocTaRequestAdvanceViaInternetIndicator;
	private String floorPercentUi;
	private boolean balloonIndicator;
	private boolean nmlsPiggyBackOrFundedHelocIndicator;
	private String rateAdjustmentPercentUi;
	private String indexMarginPercentUi;
	private boolean helocTaPeriodicCapAppliedToRepaymentPeriod;
	private String maxLifeInterestCapPercentUi;
	private boolean helocDrawPaymentBasis;
	private boolean helocTaRequestAdvanceInPersonIndicator;
}