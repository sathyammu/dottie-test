CREATE TABLE IF NOT EXISTS loan_notes (
    id SERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    loan_guid VARCHAR(255) NOT NULL,
    notes JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_notes_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES "Tenant"(id)
);
