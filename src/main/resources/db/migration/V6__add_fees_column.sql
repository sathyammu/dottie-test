alter table title_order add column fees_update_result jsonb;
alter table title_order add column mapping_errors jsonb;
alter table title_order add column accepted_at timestamp with time zone DEFAULT now();