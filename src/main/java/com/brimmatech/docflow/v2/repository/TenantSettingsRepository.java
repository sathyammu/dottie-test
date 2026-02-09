package com.brimmatech.docflow.v2.repository;

import com.brimmatech.docflow.v2.models.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Integer> {

    Optional<TenantSettings> findByTenantIdAndCategoryAndStrategy(long tenantId, String category, String strategy);

    List<TenantSettings> findByTenantId(long tenantId);

    @Query(value = "select ts.* from  tenant_settings ts inner join \"Tenant\" t on t.id = ts.tenant_id " +
            "and t.tenant_name = :globalTenantName " +
            "and category =" +
            " :category and strategy = :strategy", nativeQuery = true)
    Optional<TenantSettings> findGlobalSetting(String globalTenantName,  String category, String strategy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE tenant_settings " +
            "SET meta = CAST(:base64EncodedPrompt AS jsonb) " +
            "WHERE tenant_id = :tenantId " +
            "AND category = :category " +
            "AND strategy = :strategy", nativeQuery = true)
    int updateSystemPrompt(long tenantId,
                           String base64EncodedPrompt,
                           String category,
                           String strategy);


    @Transactional
    @Modifying
    void deleteByTenant_Id(long newTenant);

    @Query("SELECT CASE WHEN COUNT(ts) > 0 THEN true ELSE false END " +
            "FROM TenantSettings  ts " +
            "WHERE ts.tenant.id = :tenantId AND ts.category = 'sys_prompts'")
    boolean existsSysPromptForTenant(@Param("tenantId") Long tenantId);


    @Query(value = """
    SELECT json_array_elements(ts.meta::json->'fields') AS fields
    FROM tenant_settings ts
    WHERE ts.tenant_id = :tenantId
      AND ts.category = 'purchase_advice'
      AND ts.strategy = 'missing_fields'
""", nativeQuery = true)
    @Deprecated
    List<String> fetchPurchaseAdviceMissingFields(@Param("tenantId") Long tenantId);

    @Query(value = """
    SELECT json_array_elements(ts.meta::json->'fields') AS fields
    FROM tenant_settings ts
    INNER JOIN "Tenant" t ON t.id = ts.tenant_id
    WHERE t.tenant_name = :globalTenantName
     AND ts.category = 'purchase_advice'
      AND ts.strategy = 'missing_fields'update
""", nativeQuery = true)
    @Deprecated
    List<String> fetchGlobalPurchaseAdviceMissingFields(@Param("globalTenantName") String globalTenantName,
                                                        @Param("category") String category,
                                                        @Param("strategy") String strategy);


    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM tenant_settings ts " +
            "WHERE ts.tenant_id = :tenantId " +
            "AND ts.category = 'download' " +
            "AND ts.strategy = 'isActive' " +
            "AND ts.meta ->> 'active' = 'true'",
            nativeQuery = true)
    boolean isActiveDownload(@Param("tenantId") Long tenantId);


}
