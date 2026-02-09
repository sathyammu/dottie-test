package com.brimmatech.docflow.dfcommon.Tenant;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantEntityRepository extends JpaRepository<TenantEntity, Long> {

    /**
     * If you use this method , you'll be fired! use findById instead.
     * @param tenantName
     * @return
     */
    @Deprecated
    Optional<List<TenantEntity>> findByTenantNameIgnoreCase(String tenantName);

    @Query("SELECT t FROM TenantEntity t WHERE (:status = 'ALL' OR (:status = 'ARCHIVED' AND t.isArchived = true)) " +
            "AND (:search IS NULL OR t.tenantName ILIKE %:search%)")
    Page<TenantEntity> findTenants( String status, String search, Pageable pageable);

    List<TenantEntity> findAllByIsActiveTrueOrderByTenantNameAsc();

    List<TenantEntity> findAllByOrderByTenantNameAsc();

    List<TenantEntity> findAllBySendLoanSummaryTrue();

}



