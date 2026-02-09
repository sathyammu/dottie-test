package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtrqmCommon{
	private String standardQmStatusNegativeAmortization;
	private String gseagencyQmStatusCoveredLoan;
	private String generalAtrStatusAlimony;
	private String gseagencyQmStatusOverall;
	private String standardQmStatusLoanTerm;
	private String gseagencyQmStatusChildSupport;
	private String standardQmStatusIncome;
	private String standardQmStatusResidualIncome;
	private String gseagencyQmStatusBalloonPayment;
	private String standardQmStatusPriceLimit;
	private String gseagencyQmStatusAssets;
	private String standardQmStatusDti;
	private String generalAtrStatusDti;
	private String gseagencyQmStatusLoanTerm;
	private String generalAtrStatusResidualIncome;
	private String generalAtrStatusCoveredLoan;
	private String gseagencyQmStatusPointsFeesLimit;
	private String gseagencyQmStatusCreditHistory;
	private String standardQmStatusMtgRelatedObligations;
	private String generalAtrStatusChildSupport;
	private String standardQmStatusPointsFeesLimit;
	private Object startingAdjustedRateMaxBonaFideDiscountPoint;
	private String gseagencyQmStatusResidualIncome;
	private String gseagencyQmStatusSimultaneousLoan;
	private String generalAtrStatusMtgRelatedObligations;
	private Object requiredServicesLenderSelectedAmt;
	private String standardQmStatusBalloonPayment;
	private String standardQmStatusCoveredLoan;
	private String standardQmStatusAlimony;
	private String gseagencyQmStatusIncome;
	private String standardQmStatusEmployment;
	private String gseagencyQmStatusEmployment;
	private String gseagencyQmStatusDebtObligations;
	private Object bonaFideDiscountPointAmount;
	private String standardQmStatusPrepaymentPenalty;
	private String generalAtrStatusDebtObligations;
	private String standardQmStatusSimultaneousLoan;
	private String standardQmStatusAssets;
	private String standardQmStatusInterestOnly;
	private String gseagencyQmStatusMtgRelatedObligations;
	private String generalAtrStatusCreditHistory;
	private String generalAtrStatusEmployment;
	private Object titleServicesLenderTitleinsuranceFee;
	private String standardQmStatusChildSupport;
	private Object bonaFideDiscountPoint;
	private String gseagencyQmStatusDti;
	private String gseagencyQmStatusNegativeAmortization;
	private String gseagencyQmStatusInterestOnly;
	private boolean usePriceBasedLimitTest;
	private String standardQmStatusDebtObligations;
	private String gseagencyQmStatusAlimony;
	private String gseagencyQmStatusPrepaymentPenalty;
	private Object rateReductionMaxBonaFideDiscountPoint;
	private String standardQmStatusOverall;
	private String generalAtrStatusOverall;
	private String standardQmStatusCreditHistory;
	private Object aporMaxBonaFideDiscountPoint;
	private String generalAtrStatusSimultaneousLoan;
	private String generalAtrStatusIncome;
	private String generalAtrStatusAssets;
}