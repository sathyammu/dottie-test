CREATE TABLE "polled_loans" (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ NOT NULL,
    loan_identifiers TEXT,
    CONSTRAINT fk_polled_loans_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES "Tenant" (id)
);