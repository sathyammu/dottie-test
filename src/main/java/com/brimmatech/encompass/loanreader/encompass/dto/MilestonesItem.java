package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MilestonesItem{
	private int duration;
	private boolean reviewedIndicator;
	private boolean doneIndicator;
	private String name;
	private int days;
	private MilestoneSetting milestoneSetting;
	private String id;
	private LoanAssociate loanAssociate;
	private String startDate;
	private String roleRequired;
}