package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactsItem{
	private String address;
	private String city;
	private String postalCode;
	private String name;
	private String contactType;
	private String state;
	private String companyId;
	private String phone;
	private String loginId;
	private String email;
}