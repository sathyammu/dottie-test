package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hud1Es{
	private int hud1EsItemizesTotalLines;
	private Object nonEscrowCostsYearly;
	private List<Hud1EsDueDatesItem> hud1EsDueDates;
	private boolean hud1EsItemizesUseItemizeEscrowIndicator;
	private String escrowFirstPaymentDateType;
	private List<Hud1EsDatesItem> hud1EsDates;
	private List<Hud1EsItemizesItem> hud1EsItemizes;
	private Object escrowPaymentYearly;
	private List<Hud1EsSetupsItem> hud1EsSetups;
	private Object startingBalance;
}