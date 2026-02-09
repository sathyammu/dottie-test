package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FhaVaLoan{
	private Object fhaDownPayment;
	private int fhaYearsMonthlyInsurance;
	private Object fhaClosingCost;
	private Object conventionalMortgageAmount;
	private Object conventionalMonthlyPayment;
	private Object fhaUmip;
	private Object fhaMortgageAmount;
	private EnergyEfficientMortgage energyEfficientMortgage;
	private int conventionalMaxYearsMi;
	private Object fhaMortgageAmountUmip;
	private int conventionalLoanTerm;
	private Object totalClosingCost;
	private Object nonRealtyAndOtherItems;
	private Object conventionalLtv;
	private Object fhaMmi;
	private boolean useDefaultLenderInfo;
	private Object conventionalMmi;
	private Object fhaInterestRate;
	private Object fhaSalesPrice;
	private Object conventionalInterestRate;
	private Object conventionalSalesPrice;
	private Object conventionalClosingCost;
	private int fhaLoanTerm;
	private Object fhaLtv;
	private Object sellerPaidClosingCost;
	private Object conventionalDownPayment;
	private Object fhaMonthlyPayment;
}