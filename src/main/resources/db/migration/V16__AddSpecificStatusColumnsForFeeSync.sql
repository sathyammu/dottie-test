alter table title_order rename column sync_status to fee_update_status;
alter table title_order add column order_creation_status text;