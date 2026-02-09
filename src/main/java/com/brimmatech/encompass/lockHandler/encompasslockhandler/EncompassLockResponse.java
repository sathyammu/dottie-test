package com.brimmatech.encompass.lockHandler.encompasslockhandler;

import lombok.Data;

@Data
public class EncompassLockResponse {
	private Resource resource;
	private String lockTime;
	private String id;
	private String lockType;
	private String userId;
}