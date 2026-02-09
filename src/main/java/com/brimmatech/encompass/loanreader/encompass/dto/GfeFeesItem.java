package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GfeFeesItem{
	private int gfeFeeIndex;
	private Object otherAmount;
	private String gfeFeeType;
}