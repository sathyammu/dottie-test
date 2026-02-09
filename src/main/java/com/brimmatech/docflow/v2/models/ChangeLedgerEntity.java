package com.brimmatech.docflow.v2.models;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.brimmatech.docflow.v2.models.converters.TaskBusinessFlowNameConverter;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "\"change_ledger\"")
@EntityListeners(AuditingEntityListener.class)
public class ChangeLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "loan_identifier")
    private String loanIdentifier;

    @ManyToOne
    @JoinColumn(name = "tenant_id", updatable = false)
    private TenantEntity tenant;

    @Column(columnDefinition = "json", name = "doc_qualifier")
    @Type(JsonType.class)
    private JsonNode docQualifier;

    @Column(columnDefinition = "json", name = "doc_meta_data")
    @Type(JsonType.class)
    private JsonNode docMetaData;

    @Column(name = "event_time")
    private String eventTime;

    @CreatedDate
    @Column(name = "created_at")
    private String createdAt;

    private String logs;

    @Column(name = "event_type")
    @Enumerated(EnumType.STRING)
    private TenantSettingsMeta.TaskTriggeringEventType eventType;

    @Column(name = "loan_number")
    private String loanNumber;

    @Column(name = "flow_name")
    @Enumerated(EnumType.STRING)
    private TenantSettingsMeta.TaskBusinessFlowName flowName;

    @Column(columnDefinition = "json", name = "thread_id")
    @Type(JsonType.class)
    private AssistantThread threadId;

    @Builder
    public record AssistantThread(String threadId){};
}
