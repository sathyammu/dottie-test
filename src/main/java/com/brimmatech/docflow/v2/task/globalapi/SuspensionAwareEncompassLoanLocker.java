package com.brimmatech.docflow.v2.task.globalapi;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.v2.task.delegates.TaskDelegate;
import lombok.extern.slf4j.Slf4j;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;

@Slf4j
public class SuspensionAwareEncompassLoanLocker {

    //Try to use this as a component so we can reduce the no of args. Use LoanContextProvider for getting accessToken.
    public <R> SubmittableJobOutcome<R> run(TaskDelegate<?, ?> delegate,
                                            Task task,
                                            String businessKey,
                                            String loanGuid,
                                            EncompassLockApi lockApi,
                                            WorkCall<R> work, String accessToken, TenantEntity tenant) {
        String lockToken = null;
        boolean acquired = false;
        try {
            var res = lockApi.tryAcquire(loanGuid, businessKey, accessToken, tenant);
            if (!res.acquired()) {
                if (res.httpStatus() == 409) {
                    return new SubmittableJobOutcome.Suspended<>("LOCK_NOT_AVAILABLE",
                            new SubmittableJobOutcome.JobStatus(businessKey, loanGuid));
                }
                return new SubmittableJobOutcome.Failed<>(new IllegalStateException(
                        "Acquire failed: http=%d msg=%s".formatted(res.httpStatus(), res.message())));
            }
            acquired = true;
            lockToken = res.lockToken();

            R result = work.execute(businessKey, lockToken);

            try {
                lockApi.release(loanGuid, lockToken, accessToken, tenant);
                return new SubmittableJobOutcome.Completed<>(result);
            } catch (Exception releaseErr) {
                return new SubmittableJobOutcome.Suspended<>("RELEASE_FAILED_RETRY_LATER",
                        new SubmittableJobOutcome.JobStatus(businessKey, lockToken));
            }

        } catch (Exception ex) {
            return new SubmittableJobOutcome.Failed<>(ex);
        } finally {
            if (acquired && lockToken != null) {
                try {
                    lockApi.release(loanGuid, lockToken, accessToken, tenant);
                } catch (Exception unableToReleaseLock) {
                    log.debug("Unable to release lock", unableToReleaseLock);
                }
            }
        }
    }

    @FunctionalInterface
    public interface WorkCall<R> {
        R execute(String businessKey, String lockToken) throws Exception;
    }


}
