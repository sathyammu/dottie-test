package com.brimmatech.docflow.approvaldisclosure;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
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
@Table(name = "approval_disclosure")
@EntityListeners(AuditingEntityListener.class)
public class ApprovalDisclosureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "loan_number")
    private String loanNumber;

    @ManyToOne
    @JoinColumn(name = "tenant_id", updatable = false)
    private TenantEntity tenant;

    @Column(name = "status")
    private String status;

    @CreatedDate
    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private ZonedDateTime lastModifiedDate;

    @Column(name = "loan_folder")
    private String loanFolder;

    @Column(name = "flow_name")
    private String flowName;
}
