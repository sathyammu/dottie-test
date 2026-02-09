package com.brimmatech.encompass.loanreader;

import java.util.List;
import lombok.Data;

@Data
public class EncompassUserResponse {
	private String lastName;
	private String comments;
	private String fullName;
	private List<PersonasItem> personas;
	private CcSite ccSite;
	private List<String> userIndicators;
	private String workingFolder;
	private String firstName;
	private Organization organization;
	private String subordinateLoanAccess;
	private String id;
	private String peerLoanAccess;
	private boolean personalStatusOnline;
	private String email;
}