package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationsItem{
	private Coborrower coborrower;
	private Borrower borrower;
	private String legacyId;
	private String id;
	private String borrowerPairId;
	private Tax4506 tax4506;
    private String applicationId;
	private List<Vod> vods;
}