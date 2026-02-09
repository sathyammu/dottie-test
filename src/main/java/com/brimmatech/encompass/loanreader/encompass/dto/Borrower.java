package com.brimmatech.encompass.loanreader.encompass.dto;

import lombok.Data;

import java.util.List;

@Data
public class Borrower{
	private boolean currentEmploymentDoesNotApply;
	private boolean bankruptcyIndicatorChapterTwelve;
	private boolean bankruptcyIndicatorChapterThirteen;
	private String emailAddressText;
	private String workEmailAddress;
	private String fullName;
	private String birthDate;
	private String maritalStatusType;
	private List<EmploymentItem> employment;
	private boolean isBorrower;
	private String firstNameWithMiddleName;
	private boolean bankruptcyIndicatorChapterSeven;
	private String firstName;
	private String lastName;
    private String middleName;
	private String applicantType;
	private String legacyId;
	private boolean bankruptcyIndicatorChapterEleven;
	private String taxIdentificationIdentifier;
	private String id;
	private String fullNameWithSuffix;
	private String fannieFirstName;
	private Boolean specialBorrowerSellerRelationshipIndicator;
}