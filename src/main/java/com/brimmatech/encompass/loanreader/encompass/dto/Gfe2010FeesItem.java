package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gfe2010FeesItem{
	private String gfe2010FeeType;
	private int gfe2010FeeIndex;
	private String gfe2010FeeParentType;
	private boolean sellerCreditIndicator2015;
	private boolean borrowerDidShopForIndicator2015;
	private boolean borrowerCanShopForIndicator2015;
	private Object borrowerPtc2015;
	private Object borrowerPac2015;
	private Object sec32PointsAndFees2015;
	private Object totalFeeAmount2015;
	private Object borrowerAmountPaid2015;
	private String ptbType;
	private boolean sellerObligatedIndicator2015;
	private Object selPaidAmount;
	private Object sellerPac2015;
	private Object gfeAmount;
	private boolean simultaneousIssuanceIndicator2015;
	private Object undiscountedInsurance2015;
	private Object borrowerFinanced2015;
	private Object sellerObligatedAmount2015;
	private Object sellerPoc2015;
	private Object borrowerPoc2015;
	private boolean pocPtcIndicator;
	private boolean aprIndicator;
	private Object sellerAmountPaid2015;
	private boolean financedIndicator;
	private Object amount;
	private String description;
	private boolean titleServiceSelectIndicator;
}