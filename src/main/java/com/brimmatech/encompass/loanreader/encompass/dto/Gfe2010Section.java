package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gfe2010Section{
	private String line1104;
	private boolean section1100BorrowerSelectIndicator1;
	private String projectedPaymentTableColumns;
	private boolean loCompensationItemizeFeesIndicator;
	private boolean section800ItemizeFeesIndicator;
	private boolean section1100ItemizeFeesIndicator;
	private boolean loCompensationUseLoCompensationToolIndicator;
	private Object line1101BorPaidTotal;
}