package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanEstimate2{
	private int totalClosingCosts;
	private int estimatedTotalPayoffsAndPaymentsAmount;
	private int prepaidsSubTotal;
	private int sellerCreditAmount;
	private int taxesGovFeesSubTotal;
	private String fromOrToBorrower;
	private int originationChargesSubTotal;
	private int totalOtherCosts;
	private int totalLoanAndOtherCosts;
	private Object actualStdleTotalClosingCostJ;
	private Object downPayment;
	private boolean useAlternate;
	private Object estimatedCashToCloseAv;
	private int initialEscrowPaymentClosingSubTotal;
	private Object adjustmentsOtherCredits;
	private int otherSubTotal;
	private int totalLoanCosts;
	private Object estimatedCashToCloseSv;
	private Object thirdPartyPaymentsNotOtherwiseDisclosed;
	private Object unroundedTotalOtherCosts;
	private Object actualStdleSellerCredits;
	private int servicesYouNotShopSubTotal;
	private boolean useActualDownPaymentAndClosingCostsFinancedIndicator;
	private Object fundsForBorrower;
	private Object unroundedTotalLoanCosts;
	private int servicesYouShopSubTotal;
}