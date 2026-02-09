package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClosingDisclosure1{
	private boolean reasonPrepaymentPenalty;
	private boolean reasonInterestRatecharges;
	private String inEscrowOther;
	private boolean reasonChangeSettlementCharges;
	private boolean reasonChangeInApr;
	private Object estimatedTaxesInsuranceAssessments;
	private boolean reasonChangeInLoanProduct;
	private String inEscrowPropertyTaxes;
	private boolean reasonAdvancedReview;
	private boolean reasonClericalErrorCorrection;
	private boolean reasonOther;
	private Object totalCashToClose;
	private String inEscrowHomeownerInsurance;
	private boolean reasonChangedCircumstanceElg;
	private boolean changedCircumstanceFlag;
	private boolean reasonToleranceCure;
	private boolean reasonRevisionsReqConsumer;
}