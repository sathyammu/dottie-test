package com.brimmatech.docflow.v2.task.globalapi;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;

public interface EncompassLockApi {

    LockAcquireResult tryAcquire(String loanGuid, String businessKey, String accessToken, TenantEntity tenant) throws Exception;

    void release(String loanGuid, String lockToken, String accessToken, TenantEntity tenant) throws Exception;

    record LockAcquireResult(boolean acquired, int httpStatus, String lockToken, String message) {}

}