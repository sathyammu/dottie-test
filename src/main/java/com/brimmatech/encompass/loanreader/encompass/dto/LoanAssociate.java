package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanAssociate{
	private Role role;
	private boolean writeAccess;
	private String phone;
	private User user;
	private String email;
	private String loanAssociateType;
}