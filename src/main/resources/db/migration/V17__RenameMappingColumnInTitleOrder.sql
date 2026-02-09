alter table title_order rename column mapping_errors to fee_mapping_errors;
alter table title_order add column order_creation_errors jsonb;