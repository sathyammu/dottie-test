package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClosingDisclosure2{
	private Object servicesDidShopSubTotal;
	private Object totalOtherCostAtClosing;
	private Object sellerClosingCostBeforeClosing;
	private Object totalBorrowerPaidBeforeClosing;
	private Object prepaidsSubTotal;
	private Object totalBorrowerPaidAtClosing;
	private Object otherSubTotal;
	private Object initialEscrowSubTotal;
	private Object closingCostPaidByOthers;
	private Object borrowerClosingCostAtClosing;
	private Object taxesGovermentFeesSubTotal;
	private Object originationChargesSubTotal;
	private Object servicesDidNotShopSubTotal;
	private Object totalClosingCost;
	private Object totalOtherCostBeforeClosing;
	private Object totalOtherCost;
	private Object totalLoanCost;
	private Object sellerClosingCostAtClosing;
	private Object borrowerClosingCostBeforeClosing;
}