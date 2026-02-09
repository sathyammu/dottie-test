-- Add unique constraint to tenant_settings table
ALTER TABLE tenant_settings
ADD CONSTRAINT unique_category_strategy_tenant_id UNIQUE (category, strategy, tenant_id);

