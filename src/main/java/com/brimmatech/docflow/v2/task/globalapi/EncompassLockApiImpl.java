package com.brimmatech.docflow.v2.task.globalapi;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.exception.LoanLockException;
import com.brimmatech.encompass.lockHandler.LoanLockService;
import com.brimmatech.encompass.lockHandler.encompasslockhandler.LockResourceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EncompassLockApiImpl implements EncompassLockApi{

    private final LoanLockService loanLockService;

    @Override
    public LockAcquireResult tryAcquire(String loanGuid, String businessKey, String accessToken, TenantEntity tenant) {
        try {
            LockResourceResponse res = loanLockService.lockResource(accessToken, loanGuid, tenant);
            return new LockAcquireResult(true, 200, res.getId(), "OK");
        } catch (LoanLockException ex) {
            return new LockAcquireResult(false, 409, null, ex.getMessage());
        }
    }

    @Override
    public void release(String loanGuid, String lockToken, String accessToken, TenantEntity tenant) throws Exception {
        loanLockService.unlockAResource(accessToken, lockToken, loanGuid, tenant);
    }
}
