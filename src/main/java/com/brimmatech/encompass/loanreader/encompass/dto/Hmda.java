package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hmda{
	private String nmlsLoanOriginatorId;
	private int reportingYear;
	private String totalPointsAndFees;
	private boolean hmdaSyncAddressIndicator;
	private String totalLoanCosts;
	private String hmda2InterestOnlyIndicator;
	private String propertyValue;
	private boolean cdRequired;
	private boolean hmdaInterestOnlyIndicator;
	private String balloonIndicator;
	private String hoepaStatus;
	private String prepaymentPenaltyPeriod;
	private String businessOrCommercialPurpose;
	private String originationCharges;
	private String hmdaPropertyAddress;
	private String hmdaPropertyCity;
	private String hmdaProfileApplicationDateValue;
	private String applicationDate;
	private String loanType;
}