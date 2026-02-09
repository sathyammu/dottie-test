package com.brimmatech.docflow.superadmin;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.ZonedDateTime;

@Entity
@Table(name = "user_filter_preferences")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Data
@SuperBuilder
public class UserFilterPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lo_email")
    private String loEmail;

    @Type(JsonType.class)
    @Column(name = "user_preference_details", columnDefinition = "jsonb")
    private JsonNode userPreferenceDetails;

    @CreatedDate
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @LastModifiedDate
    @Column(name = "last_updated_at")
    private ZonedDateTime lastUpdatedAt;
}
