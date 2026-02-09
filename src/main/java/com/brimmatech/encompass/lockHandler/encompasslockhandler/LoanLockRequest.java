package com.brimmatech.encompass.lockHandler.encompasslockhandler;

import lombok.Data;

@Data
public class LoanLockRequest {
    private Resource resource;
    private String lockType;
}
