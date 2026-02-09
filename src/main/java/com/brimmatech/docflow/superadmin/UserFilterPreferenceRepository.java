package com.brimmatech.docflow.superadmin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFilterPreferenceRepository extends JpaRepository<UserFilterPreference, Long> {
    boolean existsByLoEmail(String loEmail);
    UserFilterPreference findByLoEmail(String loEmail);

    @Modifying
    @Query(value = "UPDATE user_filter_preferences " +
            "SET user_preference_details = user_preference_details || CAST(:patchJson AS jsonb), " +
            "    last_updated_at = NOW() " +
            "WHERE lo_email = :loEmail",
            nativeQuery = true)
    void patchPreferences(@Param("loEmail") String loEmail, @Param("patchJson") String patchJson);
}
