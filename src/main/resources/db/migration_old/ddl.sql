create extension ltree;

create table task_tree (id bigserial, qualifier jsonb , hierachy ltree, constraint unqiue_task_tree unique(qualifier ));
alter table task_tree rename column hierachy to levels;
alter table task_tree add column meta jsonb;
alter table task_tree alter column qualifier type text;

CREATE OR REPLACE FUNCTION ltree_invarchar(varchar) RETURNS ltree AS $$
SELECT ltree_in($1::cstring);
$$ LANGUAGE SQL IMMUTABLE;


CREATE CAST (varchar AS ltree) WITH FUNCTION ltree_invarchar(varchar) AS IMPLICIT;

alter table task_tree drop constraint unqiue_task_tree;

alter table task_tree add constraint unqiue_task_tree unique(qualifier,  levels);


create table tenant_settings (id bigserial, tenant_id int8, category text, strategey text);

alter table tenant_settings  add constraint "FK_tenant_settings" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);

alter table tenant_settings rename column strategey to strategy;

alter table tenant_settings add column meta jsonb;


alter table "Document_Processing_Results" add column no_splits int default 0 not null;

alter table "Document_Processing_Results" alter column metadata_id type int using metadata_id::integer;

alter table "Document_Processing_Results"  add constraint "FK_metadata_id" FOREIGN KEY (metadata_id) REFERENCES public.document_metadata(id);


alter table document_metadata add column document_check_sum text;

alter table document_metadata  add column file_name text;

alter table "Document_Extraction" add column page_number text;

delete from document_metadata where document_id is null cascade all;
alter table document_metadata add column run_id text;

alter table task_tree add column created_at timestamptz ;
alter table task_tree drop column updated_at ;

CREATE OR REPLACE FUNCTION convert_to_jsonb(v_input text)
RETURNS JSONB AS $$
DECLARE v_int_value JSONB DEFAULT NULL;
BEGIN
    BEGIN
        v_int_value := to_jsonb(v_input::json);
    EXCEPTION WHEN OTHERS THEN
        v_int_value := to_jsonb(v_input);
    END;
RETURN v_int_value;
END;
$$ LANGUAGE plpgsql ;


CREATE OR REPLACE FUNCTION try_cast(_in text, INOUT _out ANYELEMENT)
  LANGUAGE plpgsql AS
$func$
BEGIN
   EXECUTE format('SELECT %L::%s', $1, pg_typeof(_out))
   INTO  _out;
EXCEPTION WHEN others THEN
   -- do nothing: _out already carries default
END
$func$;


alter table "Document_Extraction" alter column doc_qualifier type json using doc_qualifier::json;
alter table "Document_Extraction" alter column extracted_json_data type jsonb using extracted_json_data::jsonb;

create table hooks (
id serial4 NOT null,
endpoint text not null,
tenant_id int,
hook_type text
);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

alter table hooks add constraint "FK_tenant_settings" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);
alter table hooks add constraint "FK_unique_hook_registrations" UNIQUE(tenant_id, hook_type) ;
alter table hooks  add column created_at timestamptz DEFAULT now();
alter table hooks  alter column tenant_id set not null;
alter table hooks  alter column hook_type set not null;
ALTER TABLE hooks
ALTER COLUMN id drop default,
ALTER TABLE hooks ALTER COLUMN id drop default,
alter column id SET DATA TYPE UUID USING (uuid_generate_v4()),
alter column id SET default uuid_generate_v4();


alter table change_ledger  add column event_type text;
alter table change_ledger  add column logs text, add column  loan_number text ;


alter table rule_batch  add column flow_name text;
alter table rule_batch alter column created_at SET DEFAULT now();
delete from rule_batch where created_at is null ;
delete from rule_entity  re where re.id in (select re.id from rule_entity re inner join rule_batch rb on
re.batch_id = rb.id and
rb.created_at is null);
alter table hooks drop constraint "FK_unique_hook_registrations";
alter table change_ledger  add column flow_name text;

alter table rule_batch  add column flow_name text;
alter table rule_batch alter column created_at SET DEFAULT now();
delete from rule_batch where created_at is null ;
delete from rule_entity  re where re.id in (select re.id from rule_entity re inner join rule_batch rb on
re.batch_id = rb.id and
rb.created_at is null);

alter table tenant_rule  add column flow_names text;
alter table tenant_rule  add column is_active boolean default false;


ALTER TABLE public.econsent_tracker ADD CONSTRAINT fk6friltxfpdus47ahiqnadvnpn FOREIGN KEY (change_ledger_id) REFERENCES public.change_ledger(id);
ALTER TABLE public.econsent_tracker ADD CONSTRAINT fkfolq8ndre4yk45dyj1rcg5bbj FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


CREATE SEQUENCE public.eConsent_tracker_SEQ
    AS integer
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


alter table hooks drop constraint uk6t3jk2e1l92db4d6bp5q9hrau

alter table hooks add constraint uniqueHooks unique(tenant_id, hook_type)


alter table "Document_Extraction" alter column document_confidence type NUMERIC;
alter table "Document_Extraction" add column doc_process_id int references "Document_Processing_Results"(id)
ALTER TABLE public.econsent_tracker ADD CONSTRAINT fk6friltxfpdus47ahiqnadvnpn FOREIGN KEY (change_ledger_id) REFERENCES public.change_ledger(id);
ALTER TABLE public.econsent_tracker ADD CONSTRAINT fkfolq8ndre4yk45dyj1rcg5bbj FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


CREATE SEQUENCE public.eConsent_tracker_SEQ
    AS integer
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


alter table hooks drop constraint uk6t3jk2e1l92db4d6bp5q9hrau

alter table hooks add constraint uniqueHooks unique(tenant_id, hook_type)


alter table "Document_Extraction" alter column document_confidence type NUMERIC;
alter table "Document_Extraction" add column doc_process_id int references "Document_Processing_Results"(id)



CREATE TABLE public.rule_audit (
	id int4 GENERATED BY DEFAULT AS IDENTITY( INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1 NO CYCLE) NOT NULL,
	loan_number varchar(255) NULL,
	modified_at timestamptz(6) NULL,
	modified_by varchar(255) NULL,
	modified_rule_data json NULL,
	rule_batch_id int4 NULL,
	tenant_id int4 NULL,
	CONSTRAINT rule_audit_pkey PRIMARY KEY (id),
	CONSTRAINT ukcl2xhe79jmxecmcnm00obhv0s UNIQUE (rule_batch_id)
);


-- public.rule_audit foreign keys

ALTER TABLE public.rule_audit ADD CONSTRAINT fk6hr1o38t2y76g5fm4dtxj8qma FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);
ALTER TABLE public.rule_audit ADD CONSTRAINT fkg3i6vrwjtbcrhnhk6ywquthbx FOREIGN KEY (rule_batch_id) REFERENCES public.rule_batch(id);

alter table rule_batch  add column loan_users json;

alter table rule_batch  add column status varchar(255);
alter table rule_batch  add column loan_users json;

alter table rule_batch  add column status varchar(255);

ALTER TABLE "Tenant"
ALTER COLUMN id
drop identity;

ALTER TABLE "Tenant"
ALTER COLUMN id
ADD GENERATED BY DEFAULT AS identity;

ALTER TABLE "Tenant"
ALTER COLUMN id
drop identity;

ALTER TABLE "Tenant"
ALTER COLUMN id
ADD GENERATED BY DEFAULT AS identity;

alter table "Document_Extraction"   add column confidence_lines jsonb default null;


ALTER TABLE public.rule_batch ADD milestone varchar(255) NULL;


CREATE TABLE public.document_upload_logs (
	id serial4 NOT NULL,
	full_name varchar(255) NULL,
	email_address varchar(255) NULL,
	organisation varchar(255) NULL,
	operation_location varchar(255) NULL,
	blob_folder_name varchar(255) NULL,
	document_name varchar(255) NULL,
	status varchar(50) NULL,
	start_time timestamptz NULL,
	end_time timestamptz NULL
);

--FROM COMMIT d4cd356d9fa58d5b1f42cc606b8f465f501bcc9d


create index task_tree_meta_gin_idx on task_tree USING GIN (meta);
CREATE INDEX idx_task_tree_levels ON task_tree USING BTREE (nlevel(levels));
CREATE INDEX idx_task_tree_createed ON task_tree (created_at);


-- public.task_pretty source



-- public.task definition

-- Drop table

-- DROP TABLE public.task;




CREATE OR REPLACE FUNCTION convert_to_jsonb(v_input text)
RETURNS JSONB AS $$
DECLARE v_int_value JSONB DEFAULT NULL;
BEGIN
    BEGIN
        v_int_value := to_jsonb(v_input::json);
    EXCEPTION WHEN OTHERS THEN
        v_int_value := to_jsonb(v_input);
    END;
RETURN v_int_value;
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

alter table task add column input_json jsonb  generated always as
	(convert_to_jsonb(input))
stored;


create index idx_task_input_json on task using GIN(input_json);

-- public.task_pretty source

CREATE OR REPLACE VIEW public.task_pretty
AS SELECT topic,
    sequence,
    identifier,
    created,
    completed,
        CASE
            WHEN state = 0::numeric THEN 'ACTIVE'::text
            WHEN state = 1::numeric THEN 'READY'::text
            WHEN state = 2::numeric THEN 'EXPIRED'::text
            WHEN state = 3::numeric THEN 'FAILED'::text
            WHEN state = 4::numeric THEN 'SUSPENDED'::text
            WHEN state = 5::numeric THEN 'FILTERED'::text
            WHEN state = 6::numeric THEN 'SUCCEEDED'::text
            WHEN state = 8::numeric THEN 'REDUNDANT'::text
            WHEN descent = 3::numeric THEN 'FAILED'::text
            WHEN descent = 4::numeric THEN 'SUSPENDED'::text
            WHEN descent = 5::numeric THEN 'FILTERED'::text
            WHEN descent = 6::numeric THEN 'SUCCEEDED'::text
            ELSE (('unexpected value: '::text || state) || ' with descent of '::text) || descent
        END AS state,
        CASE
            WHEN state = 8::numeric THEN 'YES'::text
            ELSE 'NO'::text        END AS recreated,
    input,
    output,
    input_json
   FROM task;

  alter table document_metadata add column created_at timestamptz;
  ALTER TABLE public.rule_batch ADD milestone varchar(255) NULL;

alter table "Document_Processing_Results" add column document_splits jsonb default null;

--02/04/2025 - Saro

create table content_understanding_analyzers (document_type text, analyzer_id text);
alter table content_understanding_analyzers add constraint uniq_document_analyzer unique(document_type, analyzer_id);
alter table content_understanding_analyzers  add column id serial;
alter table content_understanding_analyzers add primary key (id)

--- add two end keys
AZURE_CONTENT_UNDERSTANDING_ENDPOINT
AZURE_CONTENT_UNDERSTANDING_API_KEY

--07/04/2025
ALTER TABLE public.document_metadata ADD loan_number_details jsonb NULL;

--10/04/2025
CREATE TABLE user_preference (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    user_preference_details JSONB,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES application_users(application_user_id)
);


ALTER TABLE user_preference
ADD COLUMN created_at TIMESTAMPTZ NULL,
ADD COLUMN last_updated_at TIMESTAMPTZ NULL;

-- 18/04/2025; Saro
create table classification_fallback_providers (id serial, endpoint text, model_id text, key text, ranking int);
alter table classification_fallback_providers add constraint uniq_cfp_model_id unique(model_id);
alter table classification_fallback_providers add primary key (id)

--23/04-2025: Saro

alter table change_ledger add column thread_id jsonb;
alter table change_ledger  add constraint uniq_change_ledger_threads unique(loan_number, flow_name, event_type, thread_id);


--05/05-2025: Saro

alter table content_understanding_analyzers  add column profile text default null;

ALTER TABLE content_understanding_analyzers
ADD CONSTRAINT content_understanding_analyzers_uniq UNIQUE NULLS NOT DISTINCT (document_type, analyzer_id, profile);

-- Rule batch changes.
ALTER TABLE rule_batch ADD COLUMN folder_name varchar(255);
ALTER TABLE rule_batch ADD COLUMN is_reviewed boolean default false;

--29/04/2025
CREATE TABLE public.investors (
	investor_id bigserial NOT NULL,
	investor_name varchar(255) NOT NULL,
	is_purchase_advice_applicable bool NULL,
	is_purchase_suspension_applicable bool NULL,
	is_purchase_advice_active bool NULL,
	is_purchase_suspension_active bool NULL,
	meta_data text NULL,
	tenant_id int8 NULL,
	CONSTRAINT investors_pkey PRIMARY KEY (investor_id)
);


-- public.investors foreign keys

ALTER TABLE public.investors ADD CONSTRAINT "fk_investors_Tenant" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);

-- 12-05-2025
ALTER TABLE document_metadata
ADD control_mode VARCHAR(20);

--16-05-2025
CREATE TABLE public.vallia_assist (
	id bigserial NOT NULL,
	process_type varchar(255) NULL,
	vector_id varchar(255) NULL,
	thread_id varchar(255) NULL,
	loan_id varchar(255) NULL,
	borrower_name varchar(255) NULL,
	tenant_id int4 NULL,
	created_at timestamptz DEFAULT now() NULL,
	CONSTRAINT vallia_assist_pkey PRIMARY KEY (id)
);


-- public.vallia_assist foreign keys

ALTER TABLE public.vallia_assist ADD CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


-- 06/03/2025

ALTER TABLE document_metadata
ADD COLUMN user_id UUID;

ALTER TABLE document_metadata
ADD CONSTRAINT fk_user
FOREIGN KEY (user_id)
REFERENCES public.application_users(application_user_id);

--29-05-2025
ALTER TABLE change_ledger
ADD COLUMN doc_meta_data JSONB;

--23-06-2025

-- public.user_info definition

-- Drop table

-- DROP TABLE public.user_info;

CREATE TABLE public.user_info (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	thread_id varchar(255) NULL,
	user_id uuid NULL,
	CONSTRAINT user_info_pkey PRIMARY KEY (id)
);


-- public.user_info foreign keys

ALTER TABLE public.user_info ADD CONSTRAINT user_info_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.application_users(application_user_id);