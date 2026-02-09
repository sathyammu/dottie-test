package com.brimmatech.encompass.lockHandler;

import com.brimmatech.docflow.common.beanhelper.BeanHelper;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.encompass.lockHandler.encompasslockhandler.EncompassLockResponse;
import com.brimmatech.encompass.lockHandler.encompasslockhandler.LockResourceResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class LoanLockService {

    private final BeanHelper beanHelper;

    public Boolean validateForLoanLock(String accessToken,TenantEntity tenant,String loanId) {

        log.info("Validating loan for lockId :{}",loanId);

        Boolean isLoanLocked = Boolean.FALSE;

        List<EncompassLockResponse> lockResponse = retrieveLoanLockStatus(accessToken,loanId,tenant);

        if(!lockResponse.isEmpty()){
            isLoanLocked = Boolean.TRUE;
        }

        return isLoanLocked;
    }
    
    public List<EncompassLockResponse> retrieveLoanLockStatus(String encompassToken, String loanGuid, TenantEntity tenant) {
        log.info("Retrieving lock status for a loanId :{} ",loanGuid);

        LoanLockHandler loanLockHandler = beanHelper
                .getLoanLockHandler(tenant.getSor().getSystemOfRecordName().concat("-lock-handler"));

        return loanLockHandler.retrieveLock(encompassToken, loanGuid);
    }

    public LockResourceResponse lockResource(String encompassToken, String loanGuid, TenantEntity tenant) {
        log.info("Locking resource. loanId: {}", loanGuid);

        LoanLockHandler loanLockHandler = beanHelper
                .getLoanLockHandler(tenant.getSor().getSystemOfRecordName().concat("-lock-handler"));

        return loanLockHandler.lockResource(encompassToken, loanGuid);
    }

    public void unlockAResource(String encompassToken,String lockId, String loanGuid, TenantEntity tenant){

        LoanLockHandler loanLockHandler = beanHelper
                .getLoanLockHandler(tenant.getSor().getSystemOfRecordName().concat("-lock-handler"));

         loanLockHandler.unlockResource(encompassToken,lockId,loanGuid);
    }
    
}
