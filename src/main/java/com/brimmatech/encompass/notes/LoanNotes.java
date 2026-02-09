package com.brimmatech.encompass.notes;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "loan_notes")
public class LoanNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", updatable = false)
    private TenantEntity tenant;

    @Column(name = "loan_guid")
    private String loanGuid;

    @Column(name = "notes", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private JsonNode notes;

    @CreatedDate
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated_at")
    private ZonedDateTime lastUpdatedAt;
}

