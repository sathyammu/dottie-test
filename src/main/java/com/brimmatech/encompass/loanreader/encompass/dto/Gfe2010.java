package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gfe2010{
	private Object totalSettlementCharges;
	private Object section1400TotalSellerPaidAmount;
	private Object section800TotalBorrowerPaidAmount;
	private Object section800TotalPaidAmount;
	private Object section1400TotalPaidAmount;
	private boolean escrowChargeAllPropertyTaxesIndicator;
	private Object requiredTaxServiceFee;
	private Object section1400TotalBorrowerPaidAmount;
	private Object allOtherServiceAmount;
	private Object section1100TotalPaidAmount;
	private Object requiredCreditReportFee;
	private Object section1100TotalSellerPaidAmount;
	private boolean escrowChargeAllInsuranceIndicator;
	private boolean escrowChargeOtherIndicator;
	private String maxLifeInterestCapPercentUi;
	private Object titleServiceAmount;
	private List<Gfe2010WholePocsItem> gfe2010WholePocs;
	private Object homeownerInsurance;
	private String loanOriginatorName;
	private Object requiredServicesAmount;
	private List<Gfe2010FeesItem> gfe2010Fees;
	private Object totalOfFinancedFees;
}