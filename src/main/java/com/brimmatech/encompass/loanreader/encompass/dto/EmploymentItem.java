package com.brimmatech.encompass.loanreader.encompass.dto;

import lombok.Data;

@Data
public class EmploymentItem{
	private String country;
	private String addressStreetLine1;
	private String employerName;
	private String addressState;
	private boolean currentEmploymentIndicator;
	private String sourceOfIncomeData;
	private String altId;
	private String addressPostalCode;
	private boolean militaryEmployer;
	private boolean noLinkToDocTrackIndicator;
	private String phoneNumber;
	private String countryCode;
	private boolean printAttachmentIndicator;
	private String id;
	private String addressCity;
	private String urla2020StreetAddress;
	private boolean selfEmployedIndicator;
	private int timeInLineOfWorkYears;
}