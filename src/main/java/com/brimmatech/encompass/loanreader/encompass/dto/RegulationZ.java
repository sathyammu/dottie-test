package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegulationZ{
	private String interestAccrual;
	private String aprPercentUi;
	private String phonePreparedBy;
	private String namePreparedBy;
	private String zeroPercentPaymentOption;
	private String paymentFrequencyType;
	private boolean gfeRateLockRedisclosureRequiredIndicator;
	private boolean interestOnlyIndicator;
	private boolean gfeChangedCircumstanceIndicator;
	private boolean hud1ToleranceViolatedIndicator;
	private String regzTableType;
	private Double miMonthlyPaymentLevel1;
	private String gfeApplicationDate;

}