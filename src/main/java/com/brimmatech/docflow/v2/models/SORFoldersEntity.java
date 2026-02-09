package com.brimmatech.docflow.v2.models;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "\"System_of_Record_Folders\"")
@EntityListeners(AuditingEntityListener.class)
public class SORFoldersEntity {
    @Id
    private Integer id;

    @Column(name = "folder_name")
    private String folderName;

    @Column(name = "sor_folder_id")
    private String sorFolderId;

    @Column(name = "is_listened_by_vallia_doc_flow")
    private Boolean isListenedByValliaDocFlow;

    @ManyToOne
    @JoinColumn(name = "tenant_id", updatable = false)
    private TenantEntity tenant;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "last_updated_at")
    private ZonedDateTime lastUpdatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_updated_by")
    private String lastUpdatedBy;

}
