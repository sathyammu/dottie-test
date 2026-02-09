package com.brimmatech.docflow.v2.task.alert;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.superadmin.AdminService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 *  See @LoanSummaryDelegate
 **/
public class LoanSummaryScheduler {

    private final TenantEntityRepository tenantEntityRepository;
    private final AdminService adminService;

    @Scheduled(cron = "${loan-summary-cron}")
    public void runDailyLoanSummary() {
        log.info("==== Loan Summary Scheduler STARTED at UTC:{} ====", ZonedDateTime.now(ZoneOffset.UTC));
        try {
            List<TenantEntity> eligibleTenantsForClassification =
                    tenantEntityRepository.findAllBySendLoanSummaryTrue();

            log.info("Eligible tenants count for classification : {} ", eligibleTenantsForClassification.size());

            eligibleTenantsForClassification.forEach(tenant -> {
                try {
                    adminService.loanSummaryAttachmentEmail(tenant.getId());
                } catch (Exception e) {
                    log.error("Failed to send loan summary for tenant id {}: {}", tenant.getId(), e.getMessage(), e);
                }
            });
        } catch (Exception ex) {
            log.error("Unhandled exception in LoanSummaryScheduler: {}", ex.getMessage(), ex);
        }
        log.info("==== Loan Summary Scheduler COMPLETED at UTC:{} ====", ZonedDateTime.now(ZoneOffset.UTC));
    }


}

