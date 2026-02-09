ALTER TABLE "System_of_Record_Folders"  alter column tenant_id type bigint;
ALTER TABLE "Tenant"  alter column id type bigint;
ALTER TABLE "change_ledger"  alter column tenant_id type bigint;
ALTER TABLE "tenant_settings"  alter column tenant_id type bigint;
ALTER TABLE "document_auto_result"  alter column tenant_id type bigint;
ALTER TABLE "investors"  alter column tenant_id type bigint;
ALTER TABLE "approval_disclosure"  alter column tenant_id type bigint;
ALTER TABLE "user_info"  alter column tenant_id type bigint;

