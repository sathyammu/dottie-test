package com.brimmatech.encompass.loanreader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BorrowerRequest {

    private String firstName;

    private String firstNameWithMiddleName;

    private String fullName;

    private String fullNameWithSuffix;

    private List<EmploymentRequest> employment;

    private boolean currentEmploymentDoesNotApply;

    private boolean bankruptcyIndicatorChapterTwelve;

    private boolean bankruptcyIndicatorChapterThirteen;

    private String emailAddressText;

    private String workEmailAddress;

    private String maritalStatusType;

    private boolean isBorrower;

    private boolean bankruptcyIndicatorChapterSeven;

    private String applicantType;

    private boolean bankruptcyIndicatorChapterEleven;

    private String taxIdentificationIdentifier;

    private String fannieFirstName;
}

