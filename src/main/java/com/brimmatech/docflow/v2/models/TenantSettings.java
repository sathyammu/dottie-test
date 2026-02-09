package com.brimmatech.docflow.v2.models;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.superadmin.dto.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.val;
import org.hibernate.annotations.Type;

@Data
@Entity
@Table(name = "tenant_settings")
public class TenantSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, name = "category")
    private String category;

    @Column(nullable = false, name = "strategy")
    private String strategy;
 
    @ManyToOne
    @JoinColumn(name = "tenant_id", updatable = false)
    private TenantEntity tenant;

    @Column(columnDefinition = "json", name = "meta")
    @Type(JsonType.class)
    private JsonNode meta;

    public void copyFrom(TenantSettings s) {
        setMeta(s.getMeta());
        setCategory(s.getCategory());
        setStrategy(s.getStrategy());
    }
    public static TenantSettings from(SettingsCategory settingsCategory) {
        val newS = new TenantSettings();
        newS.setCategory(settingsCategory.getCategory());
        newS.setStrategy(settingsCategory.getStrategy());

        return newS;
    }

}
