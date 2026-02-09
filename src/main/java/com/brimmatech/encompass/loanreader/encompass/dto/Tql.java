package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tql{
	private int driveAppVerifyScore;
	private int tqlFraudAlertsTotalLow;
	private int driveScore;
	private int driveIdVerifyScore;
	private int tqlFraudAlertsTotal;
	private int tqlFraudAlertsTotalHigh;
	private int tqlFraudAlertsTotalHighUnaddressed;
	private boolean lomaOrLomrIndicator;
	private int tqlFraudAlertsTotalMediumUnaddressed;
	private int drivePropertyVerifyScore;
	private int tqlFraudAlertsTotalLowUnaddressed;
	private int tqlFraudAlertsTotalMedium;
}