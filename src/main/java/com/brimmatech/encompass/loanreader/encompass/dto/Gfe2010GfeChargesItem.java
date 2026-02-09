package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Gfe2010GfeChargesItem{
	private int gfe2010GfeChargeIndex;
	private boolean chargeBelow10Indicator;
	private String line;
	private String description;
	private Object hudCharge;
	private Object gfeCharge;
}