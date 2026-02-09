package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Gfe2010Page{
	private Object hud1Pg1TotalSettlementCharges;
	private Object hud1Pg2TotalSettlementCharges;
	private Object hudTotalTolerance;
	private boolean hasEscrowAccountIndicator;
	private Object hud1Pg2SellerPaidClosingCostsAmount;
	private List<Gfe2010GfeChargesItem> gfe2010GfeCharges;
	private Object hud1Pg1SellerPaidClosingCostsAmount;
	private Object gfeTotalTolerance;
}