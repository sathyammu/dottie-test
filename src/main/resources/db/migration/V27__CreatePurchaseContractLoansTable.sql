CREATE TABLE purchase_contract_loans (
    id BIGSERIAL PRIMARY KEY,
    loan_number VARCHAR(255) NOT NULL UNIQUE,
    loan_id VARCHAR(255),
    task_tree_id BIGINT NULL,
    is_processed boolean,
    tenant_id BIGINT NOT NULL,
    CONSTRAINT uq_purchase_contract_loans_loan_number UNIQUE (loan_number),
    CONSTRAINT fk_purchase_contract_loans_task_tree
        FOREIGN KEY (task_tree_id) REFERENCES task_tree(id),
    CONSTRAINT fk_purchase_contract_loans_tenant
        FOREIGN KEY (tenant_id) REFERENCES "Tenant"(id)
);
