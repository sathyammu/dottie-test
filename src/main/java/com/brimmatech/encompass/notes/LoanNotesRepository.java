package com.brimmatech.encompass.notes;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanNotesRepository extends JpaRepository<LoanNotes, Integer> {

    Optional<LoanNotes> findByTenantAndLoanGuid(TenantEntity tenant, String loanGuid);
}

