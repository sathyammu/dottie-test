package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HudLoanData{
	private Object step2EPlusStep1E;
	private Object supplementalOriginationFee;
	private String criteriaForAppropriateLtvFactor;
	private Object initialDrawAtClosingTotal;
	private Object appropriateLtvFactor;
	private Object totalRehabilitationCostsFeesReserves;
	private Object rehabilitationEscrowAccount;
	private Object financeableMortgageFeesIfCharged;
}