CREATE TABLE rule_requests (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES "Tenant"(id),
    rule_spec TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deployed_at TIMESTAMP
);

ALTER TABLE bayequity_package_list ADD COLUMN IF NOT EXISTS processed_env VARCHAR(50);
