CREATE TABLE user_filter_preferences (
    id BIGSERIAL PRIMARY KEY,
    lo_email VARCHAR(255) NOT NULL UNIQUE,
    user_preference_details JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
