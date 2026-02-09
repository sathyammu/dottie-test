package com.brimmatech.encompass.loanreader.encompass.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RateLock{
	private String rateStatus;
	private String isCancelled;
	private String requestType;
	private String requestPending;
	private int daysToExtend;
	private List<LockRequestBorrowersItem> lockRequestBorrowers;
	private String requestLockType;
	private int correspondentWarehouseBankId;
	private String requestPrepayPenalty;
	private boolean fhaSecondaryResidence;
	private Object helocCreditLimit;
	private String rateRequestStatus;
	private String reLockRequestPending;
	private String extensionRequestPending;
	private String cancellationRequestPending;
	private String borrLenderPaid;
	private boolean correspondentRetainUserInputs;
	private String helocActualBalance;
	private String propertyAppraisedValueAmount;
}