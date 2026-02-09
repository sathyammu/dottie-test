package com.brimmatech.docflow.v2.repository;

import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.brimmatech.docflow.v2.models.ChangeLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeLedgerRepository extends JpaRepository<ChangeLedgerEntity,Integer> {

    List<ChangeLedgerEntity> findByLoanIdentifier(String loanIdentifier);
    List<ChangeLedgerEntity> findByLoanNumber(String loanNumber);

    List<ChangeLedgerEntity> findByLoanIdentifierAndFlowName(String loanIdentifier, TenantSettingsMeta.TaskBusinessFlowName flowName);

    ChangeLedgerEntity findFirstByLoanIdentifierOrderByIdDesc(String loanIdentifier);

}
