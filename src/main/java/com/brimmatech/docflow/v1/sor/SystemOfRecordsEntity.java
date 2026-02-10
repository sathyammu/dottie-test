package com.brimmatech.docflow.v1.sor;

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
@Table(name = "System_Of_Records")
@EntityListeners(AuditingEntityListener.class)
public class SystemOfRecordsEntity {
    @Id
    private Integer id;

    @Column(name = "system_of_record_name")
    private String systemOfRecordName;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(name = "is_active")
    private boolean isActive;

    @CreatedDate
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_updated_at")
    private ZonedDateTime lastUpdatedAt;

    @Column(name = "last_updated_by")
    private String lastUpdatedBy;
}
