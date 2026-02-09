package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Coborrower{
	private boolean bankruptcyIndicatorChapterSeven;
	private String applicantType;
	private boolean bankruptcyIndicatorChapterTwelve;
	private boolean bankruptcyIndicatorChapterThirteen;
	private String legacyId;
	private List<EmploymentItem> employment;
	private boolean bankruptcyIndicatorChapterEleven;
	private String id;
	private String birthDate;
	private String maritalStatusType;
	private boolean isBorrower;
    private String fullName;
    private String firstName;
    private String lastName;
    private String middleName;
}