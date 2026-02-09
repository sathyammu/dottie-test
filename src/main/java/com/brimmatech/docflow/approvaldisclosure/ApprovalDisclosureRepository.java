package com.brimmatech.docflow.approvaldisclosure;

import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDisclosureRepository extends JpaRepository<ApprovalDisclosureEntity,Integer> {

    List<ApprovalDisclosureEntity> findByTenant_IdAndStatusIgnoreCaseAndLoanFolderInIgnoreCaseAndFlowName(
            Long tenantId, String status, List<String> loanFolders, String flowName);

    List<ApprovalDisclosureEntity> findByLoanNumber(String loanNumber);

    List<ApprovalDisclosureEntity> findByLoanNumberAndStatusIgnoreCaseAndFlowName(String loanNumber, String status, String flowName);


}
