package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Correspondent{
	private int gracePeriodDays;
	private Object lateFeePercentage;
	private Object additionalLateFeeCharge;
	private int lfsGracePeriodCalendar;
	private int lfsLateFeeBasedOn;
	private int lfsGracePeriodDays;
	private String lateDaysEndTrigger;
	private Object lfsAmount;
	private int lfsGracePeriodStarts;
	private int lfsMaxLateDays;
	private int lfsFeeHandledAs;
	private int lfsStartOnWeekend;
	private int lfsCalculateAs;
	private int lfsGracePeriodLaterOf;
	private int lfsDayCleared;
	private int lfsIncludeDay;
	private Object lfsLateFee;
}