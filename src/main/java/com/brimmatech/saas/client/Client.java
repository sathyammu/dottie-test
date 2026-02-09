package com.brimmatech.saas.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Table(name = "client")
public class Client {

    @Id
    @Column(name = "client_id")
    private UUID clientId;
    @Column(name = "client_name")
    private String clientName;
    @Column(name = "company_name")
    private String companyName;
    @Column(name = "email")
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(name = "address_line1")
    private String addressLine1;
    @Column(name = "address_line2")
    private String addressLine2;
    private String state;
    private String city;
    private String zipcode;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "admin_comments")
    private String adminComments;
    @CreatedDate
    @Column(name = "created_at")
    private ZonedDateTime createdAt;
    @LastModifiedDate
    @Column(name = "last_updated_at")
    private ZonedDateTime lastUpdatedAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "last_updated_by")
    private String lastUpdatedBy;
    @Column(name = "is_active")
    private Boolean isActive;
}
