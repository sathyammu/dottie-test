package com.brimmatech.docflow.dfcommon.Tenant;

import com.brimmatech.docflow.v1.sor.SystemOfRecordsEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "\"Tenant\"")
@EntityListeners(AuditingEntityListener.class)
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(columnDefinition = "text",name = "doc_classifier_metadata")
    private String docClassifierMetadata;

    @Column(columnDefinition = "text",name = "sor_metadata")
    private String sorMetadata;

    @CreatedDate
    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    @LastModifiedDate
    @Column(name = "last_updated_date")
    private ZonedDateTime lastUpdatedDate;

    @Column(name = "archived_time")
    private ZonedDateTime archivedTime;

    @Column(name = "is_archived")
    private boolean isArchived;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_updated_by")
    private String lastUpdatedBy;

    @Column(columnDefinition = "text",name = "pipeline_request")
    private String pipelineRequest;

    @Column(columnDefinition = "text",name = "connection_status")
    private String connectionStatus;

    @Column(name = "send_loan_summary")
    private boolean sendLoanSummary;


    @ManyToOne
    @JoinColumn(name = "sor_id",nullable = false)
    private SystemOfRecordsEntity sor;

    @Column(name = "enhanced_condition_type")
    private String enhancedConditionType;

}
