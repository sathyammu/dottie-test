package com.brimmatech.encompass.lockHandler;

import com.brimmatech.encompass.lockHandler.encompasslockhandler.LockResourceResponse;

import java.util.List;

public interface LoanLockHandler {

    <T> List<T> retrieveLock(String accessToken,String loanGuid);

    LockResourceResponse lockResource(String accessToken, String loanGuid);

    void unlockResource(String accessToken,String lockId,String loanGuid);
}
