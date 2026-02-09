package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanEstimate1{
	private boolean reasonEligibility;
	private boolean reasonRevisions;
	private String closingCostEstimateExpirationTimeZone;
	private String inEscrowOther;
	private String closingCostEstimateExpirationTimeZoneUi;
	private Object estimatedTaxesInsuranceAssessments;
	private String inEscrowPropertyTaxes;
	private String closingCostEstimateExpirationTime;
	private boolean reasonExpiration;
	private boolean reasonDelayedSettlement;
	private boolean reasonInterestRate;
	private String rateLockExpirationTime;
	private boolean reasonOther;
	private String closingCostEstimateExpirationTimeUi;
	private String estimatedTaxesInsuranceAssessmentsUi;
	private String inEscrowHomeownerInsurance;
	private boolean reasonSettlementCharges;
	private String expirationGenericTimeZone;
	private String rateLockExpirationTimeZone;
	private Object totalEstimatedCashClose;
}