ALTER TABLE extraction_hubs
ADD COLUMN tenant_id BIGINT,
ADD CONSTRAINT fk_extraction_hubs_tenant
    FOREIGN KEY (tenant_id)
    REFERENCES "Tenant"(id);


UPDATE extraction_hubs
SET tenant_id = (
    SELECT id FROM "Tenant" t where t.tenant_name  = '__vdx__' LIMIT 1
)
WHERE tenant_id IS NULL;