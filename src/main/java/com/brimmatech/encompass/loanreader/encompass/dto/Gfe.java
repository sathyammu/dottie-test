package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Gfe{
	private boolean hasPrepaymentPenaltyIndicator;
	private String address;
	private List<GfeFeesItem> gfeFees;
	private Object totalClosingCostWithDiscount;
	private String city;
	private String postalCode;
	private String brokerRepresentative;
	private String state;
	private String brokerName;
	private Object totalMaximumCostsExpenses;
}