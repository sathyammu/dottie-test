--
-- PostgreSQL database dump
--

-- Dumped from database version 17.2
-- Dumped by pg_dump version 17.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: ltree; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS ltree WITH SCHEMA public;


--
-- Name: EXTENSION ltree; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION ltree IS 'data type for hierarchical tree-like structures';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: entity_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.entity_type AS ENUM (
    'paystub.employer',
    'urla.employerausdu.assets'
);


ALTER TYPE public.entity_type OWNER TO postgres;

--
-- Name: ltree_invarchar(character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.ltree_invarchar(character varying) RETURNS public.ltree
    LANGUAGE sql IMMUTABLE
    AS $_$
SELECT ltree_in($1::cstring);
$_$;


ALTER FUNCTION public.ltree_invarchar(character varying) OWNER TO postgres;

--
-- Name: CAST (character varying AS public.ltree); Type: CAST; Schema: -; Owner: -
--

CREATE CAST (character varying AS public.ltree) WITH FUNCTION public.ltree_invarchar(character varying) AS IMPLICIT;


--
-- Name: convert_to_jsonb(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.convert_to_jsonb(v_input text) RETURNS jsonb
    LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE
    AS $$
DECLARE v_int_value JSONB DEFAULT NULL;
BEGIN
    BEGIN
        v_int_value := to_jsonb(v_input::json);
    EXCEPTION WHEN OTHERS THEN
        v_int_value := to_jsonb(v_input);
    END;
RETURN v_int_value;
END;
$$;


ALTER FUNCTION public.convert_to_jsonb(v_input text) OWNER TO postgres;

--
-- Name: permute(anyarray); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.permute(anyarray) RETURNS SETOF anyarray
    LANGUAGE sql IMMUTABLE
    AS $_$
  SELECT (WITH RECURSIVE r(n,p,a,b)
               AS (SELECT i, $1[1:0], $1, array_upper($1,1)
                   UNION ALL
                   SELECT n / b, p || a[n % b + 1], a[1:n % b] || a[n % b + 2:b], b-1
                     FROM r
                    WHERE b > 0)
          SELECT p FROM r WHERE b=0)
  FROM generate_series(0,(array_upper($1,1))::integer-1) i;
$_$;


ALTER FUNCTION public.permute(anyarray) OWNER TO postgres;

--
-- Name: task_activation_notify_fct(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.task_activation_notify_fct() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
      BEGIN
      PERFORM pg_notify('task_notification', 'ACTIVATION ' || MD5(NEW.TOPIC));
      RETURN NEW;
      END;
      $$;


ALTER FUNCTION public.task_activation_notify_fct() OWNER TO postgres;

--
-- Name: task_notify_insert_fct(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.task_notify_insert_fct() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
      BEGIN
      PERFORM pg_notify('task_notification', 'WORK ' || MD5(NEW.TOPIC));
      RETURN NEW;
      END;
      $$;


ALTER FUNCTION public.task_notify_insert_fct() OWNER TO postgres;

--
-- Name: task_topic_drop_fct(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.task_topic_drop_fct() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
            BEGIN
            EXECUTE 'DROP TABLE ' || QUOTE_IDENT('TASK_P_' || MD5(OLD.TOPIC));
            RETURN OLD;
            END;
            $$;


ALTER FUNCTION public.task_topic_drop_fct() OWNER TO postgres;

--
-- Name: task_topic_fct(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.task_topic_fct() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
            BEGIN
            EXECUTE 'CREATE TABLE ' || QUOTE_IDENT('TASK_P_' || MD5(NEW.TOPIC)) || '
            PARTITION OF TASK
            FOR VALUES IN (' || QUOTE_LITERAL(NEW.TOPIC) || ')';
            EXECUTE 'ALTER TABLE ' || QUOTE_IDENT('TASK_P_' || MD5(NEW.TOPIC)) || '
            ADD CONSTRAINT ' || QUOTE_IDENT('TASK_TOPIC_REF_' || MD5(NEW.TOPIC)) || '
            FOREIGN KEY (TOPIC) REFERENCES TASK_TOPIC (TOPIC)';
            EXECUTE 'ALTER TABLE ' || QUOTE_IDENT('TASK_P_' || MD5(NEW.TOPIC)) || '
            ADD CONSTRAINT ' || QUOTE_IDENT('TASK_OWNER_REF_' || MD5(NEW.TOPIC)) || '
            FOREIGN KEY (OWNER) REFERENCES TASK_OWNER (OWNER)';
            RETURN NEW;
            END;
            $$;


ALTER FUNCTION public.task_topic_fct() OWNER TO postgres;

--
-- Name: task_topic_purge_all(text); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.task_topic_purge_all(text) RETURNS boolean
    LANGUAGE plpgsql
    AS $_$
            BEGIN
            EXECUTE 'TRUNCATE ' || QUOTE_IDENT('TASK_P_' || MD5($1));
            RETURN TRUE;
            END;
            $_$;


ALTER FUNCTION public.task_topic_purge_all(text) OWNER TO postgres;

--
-- Name: try_cast(text, anyelement); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.try_cast(_in text, INOUT _out anyelement) RETURNS anyelement
    LANGUAGE plpgsql
    AS $_$
BEGIN
   EXECUTE format('SELECT %L::%s', $1, pg_typeof(_out))
   INTO  _out;
EXCEPTION WHEN others THEN
   -- do nothing: _out already carries default
END
$_$;


ALTER FUNCTION public.try_cast(_in text, INOUT _out anyelement) OWNER TO postgres;

--
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: Application_Config; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Application_Config" (
    id integer NOT NULL,
    type text,
    label text,
    value text NOT NULL,
    sort_position integer NOT NULL,
    field_param text,
    default_value integer NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by text,
    last_updated_at timestamp with time zone NOT NULL,
    last_updated_by text
);


ALTER TABLE public."Application_Config" OWNER TO postgres;

--
-- Name: Application_Config_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Application_Config" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Application_Config_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);



CREATE TABLE public."System_Of_Record_Fields" (
    id integer NOT NULL,
    field_id text,
    field_name text,
    sor_id integer NOT NULL,
    tenant_id integer NOT NULL,
    is_custom_field_id boolean NOT NULL
);


ALTER TABLE public."System_Of_Record_Fields" OWNER TO postgres;

--
-- Name: System_Of_Record_Fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."System_Of_Record_Fields" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."System_Of_Record_Fields_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: System_Of_Records; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."System_Of_Records" (
    id integer NOT NULL,
    system_of_record_name character varying(255),
    metadata text,
    is_active boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(255),
    last_updated_at timestamp with time zone NOT NULL,
    last_updated_by character varying(255)
);


ALTER TABLE public."System_Of_Records" OWNER TO postgres;

--
-- Name: System_Of_Records_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."System_Of_Records" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."System_Of_Records_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: task; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task (
    topic character varying(250) NOT NULL,
    sequence bigint NOT NULL,
    identifier character varying(500) NOT NULL,
    state numeric(1,0) DEFAULT 1 NOT NULL,
    descent numeric(1,0) DEFAULT 1 NOT NULL,
    owner character varying(250),
    created timestamp without time zone DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'::text) NOT NULL,
    input text,
    output text,
    completed timestamp without time zone,
    reference character varying(500),
    input_json jsonb GENERATED ALWAYS AS (public.convert_to_jsonb(input)) STORED,
    CONSTRAINT task_descent_check CHECK (((descent >= (1)::numeric) AND (descent <= (6)::numeric))),
    CONSTRAINT task_state_check CHECK (((state >= (0)::numeric) AND (state <= (8)::numeric)))
)
PARTITION BY LIST (topic);


ALTER TABLE public.task OWNER TO postgres;

--
-- Name: task_sequence_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.task_sequence_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.task_sequence_seq OWNER TO postgres;

--
-- Name: task_sequence_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.task_sequence_seq OWNED BY public.task.sequence;

--
-- Name: Tenant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Tenant" (
    id integer NOT NULL,
    sor_id integer NOT NULL,
    doc_classifier_id integer NOT NULL,
    tenant_name character varying(255),
    is_active boolean NOT NULL,
    doc_classifier_metadata text,
    sor_metadata text,
    created_date timestamp with time zone NOT NULL,
    last_updated_date timestamp with time zone NOT NULL,
    archived_time timestamp with time zone,
    is_archived boolean NOT NULL,
    created_by character varying(255),
    last_updated_by character varying(255),
    pipeline_request text,
    connection_status text,
    rule_strategy character varying,
    email character varying,
    batch_run_time character varying(255)
);


ALTER TABLE public."Tenant" OWNER TO postgres;

--
-- Name: Tenant_Configuration; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Tenant_Configuration" (
    id integer NOT NULL,
    tenant_id integer NOT NULL,
    key character varying(256),
    value text
);


ALTER TABLE public."Tenant_Configuration" OWNER TO postgres;

ALTER TABLE public."Tenant" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Tenant_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: change_ledger; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.change_ledger (
    id integer NOT NULL,
    created_at character varying(255),
    doc_qualifier json,
    event_id character varying(255),
    event_time character varying(255),
    loan_identifier character varying(255),
    tenant_id integer,
    event_type character varying(255),
    logs character varying(255),
    loan_number character varying(255),
    flow_name character varying(255),
    thread_id jsonb
);


ALTER TABLE public.change_ledger OWNER TO postgres;

--
-- Name: change_ledger_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.change_ledger_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.change_ledger_id_seq OWNER TO postgres;

--
-- Name: change_ledger_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.change_ledger_id_seq OWNED BY public.change_ledger.id;




CREATE TABLE public.task_activation (
    topic character varying(200) NOT NULL,
    active boolean NOT NULL
);


ALTER TABLE public.task_activation OWNER TO postgres;

--
-- Name: task_owner; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_owner (
    owner character varying(250) NOT NULL,
    heartbeat timestamp without time zone DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'::text) NOT NULL
);


ALTER TABLE public.task_owner OWNER TO postgres;

--
-- Name: task_pretty; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.task_pretty AS
 SELECT topic,
    sequence,
    identifier,
    created,
    completed,
        CASE
            WHEN (state = (0)::numeric) THEN 'ACTIVE'::text
            WHEN (state = (1)::numeric) THEN 'READY'::text
            WHEN (state = (2)::numeric) THEN 'EXPIRED'::text
            WHEN (state = (3)::numeric) THEN 'FAILED'::text
            WHEN (state = (4)::numeric) THEN 'SUSPENDED'::text
            WHEN (state = (5)::numeric) THEN 'FILTERED'::text
            WHEN (state = (6)::numeric) THEN 'SUCCEEDED'::text
            WHEN (state = (8)::numeric) THEN 'REDUNDANT'::text
            WHEN (descent = (3)::numeric) THEN 'FAILED'::text
            WHEN (descent = (4)::numeric) THEN 'SUSPENDED'::text
            WHEN (descent = (5)::numeric) THEN 'FILTERED'::text
            WHEN (descent = (6)::numeric) THEN 'SUCCEEDED'::text
            ELSE ((('unexpected value: '::text || state) || ' with descent of '::text) || descent)
        END AS state,
        CASE
            WHEN (state = (8)::numeric) THEN 'YES'::text
            ELSE 'NO'::text
        END AS recreated,
    input,
    output,
    input_json
   FROM public.task;


ALTER VIEW public.task_pretty OWNER TO postgres;

--
-- Name: task_routes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_routes (
    id integer NOT NULL,
    name character varying(255),
    config jsonb
);


ALTER TABLE public.task_routes OWNER TO postgres;

--
-- Name: task_routes_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.task_routes_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.task_routes_id_seq OWNER TO postgres;

--
-- Name: task_routes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.task_routes_id_seq OWNED BY public.task_routes.id;


--
-- Name: task_topic; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_topic (
    topic character varying(250) NOT NULL
);


ALTER TABLE public.task_topic OWNER TO postgres;

--
-- Name: task_tree; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_tree (
    id integer NOT NULL,
    qualifier text,
    levels public.ltree,
    meta jsonb,
    created_at timestamp with time zone
);


ALTER TABLE public.task_tree OWNER TO postgres;

--
-- Name: task_tree_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.task_tree_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.task_tree_id_seq OWNER TO postgres;

--
-- Name: task_tree_id_seq1; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.task_tree_id_seq1
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.task_tree_id_seq1 OWNER TO postgres;

--
-- Name: task_tree_id_seq1; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.task_tree_id_seq1 OWNED BY public.task_tree.id;


--
-- Name: task_with_runids; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.task_with_runids AS
 WITH tree_with_run_id AS (
         SELECT tt.qualifier,
            (tt.meta #>> '{runId}'::text[]) AS runid,
            (tt.meta #>> '{tenantId}'::text[]) AS tenantid,
            tt.created_at,
            tt.meta
           FROM public.task_tree tt
          WHERE ((public.nlevel(tt.levels) = 1) AND ((tt.meta #>> '{runId}'::text[]) IS NOT NULL))
        ), task_with_run_ids AS (
         SELECT ((public.subpath(tt.levels, (public.nlevel(tt.levels) - 1)))::text)::integer AS trid_sequence,
            tree_with_run_id.runid,
            tree_with_run_id.tenantid,
            tt.qualifier AS tree_qualifier,
            tree_with_run_id.created_at,
            tree_with_run_id.meta
           FROM (public.task_tree tt
             JOIN tree_with_run_id ON ((tree_with_run_id.qualifier = tt.qualifier)))
          WHERE ((public.nlevel(tt.levels) > 2) AND (tt.levels OPERATOR(public.~) '*.!__END__'::public.lquery))
        )
 SELECT tp.topic,
    tp.sequence,
    tp.identifier,
    tp.created,
    tp.completed,
    tp.state,
    tp.recreated,
    tp.input,
    tp.output,
    trid.trid_sequence,
    trid.runid,
    trid.tenantid,
    trid.tree_qualifier,
    trid.created_at,
    trid.meta
   FROM (public.task_pretty tp
     JOIN task_with_run_ids trid ON ((trid.trid_sequence = tp.sequence)));


ALTER VIEW public.task_with_runids OWNER TO postgres;

--
-- Name: tenant_configuration_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tenant_configuration_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tenant_configuration_id_seq OWNER TO postgres;

--
-- Name: tenant_configuration_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tenant_configuration_id_seq OWNED BY public."Tenant_Configuration".id;

--
-- Name: tenant_settings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tenant_settings (
    id integer NOT NULL,
    category character varying(255) NOT NULL,
    meta json,
    strategy character varying(255) NOT NULL,
    tenant_id integer
);


ALTER TABLE public.tenant_settings OWNER TO postgres;

--
-- Name: tenant_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tenant_settings_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tenant_settings_id_seq OWNER TO postgres;

--
-- Name: tenant_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tenant_settings_id_seq OWNED BY public.tenant_settings.id;


--
-- Name: title_order; Type: TABLE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.transition_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transition_seq OWNER TO postgres;

--
-- Name: user_config; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_config (
    user_config_id uuid NOT NULL,
    config_value text,
    client_id uuid,
    feature_config_id uuid
);


ALTER TABLE public.user_config OWNER TO postgres;

--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31; Type: TABLE ATTACH; Schema: public; Owner: postgres

ALTER TABLE ONLY public."Tenant_Configuration" ALTER COLUMN id SET DEFAULT nextval('public.tenant_configuration_id_seq'::regclass);


--
-- Name: change_ledger id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.change_ledger ALTER COLUMN id SET DEFAULT nextval('public.change_ledger_id_seq'::regclass);

-- Name: task sequence; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ALTER COLUMN sequence SET DEFAULT nextval('public.task_sequence_seq'::regclass);


--
-- Name: task_routes id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_routes ALTER COLUMN id SET DEFAULT nextval('public.task_routes_id_seq'::regclass);


--
-- Name: task_tree id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_tree ALTER COLUMN id SET DEFAULT nextval('public.task_tree_id_seq1'::regclass);

--
-- Name: tenant_settings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_settings ALTER COLUMN id SET DEFAULT nextval('public.tenant_settings_id_seq'::regclass);

ALTER TABLE ONLY public."System_Of_Record_Fields"
    ADD CONSTRAINT "PK_System_Of_Record_Fields" PRIMARY KEY (id);


--
-- Name: System_Of_Records PK_System_Of_Records; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_Of_Records"
    ADD CONSTRAINT "PK_System_Of_Records" PRIMARY KEY (id);

--
-- Name: Tenant PK_Tenant; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant"
    ADD CONSTRAINT "PK_Tenant" PRIMARY KEY (id);

--
-- Name: task_activation TASK_ACTIVATION_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_activation
    ADD CONSTRAINT "TASK_ACTIVATION_pkey" PRIMARY KEY (topic);


--
-- Name: task_owner TASK_OWNER_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_owner
    ADD CONSTRAINT "TASK_OWNER_pkey" PRIMARY KEY (owner);


--
-- Name: task task_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_pk PRIMARY KEY (topic, sequence);


--
-- Name: change_ledger change_ledger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.change_ledger
    ADD CONSTRAINT change_ledger_pkey PRIMARY KEY (id);


--
-- Name: task_routes task_routes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_routes
    ADD CONSTRAINT task_routes_pkey PRIMARY KEY (id);


--
-- Name: Tenant_Configuration tenant_configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Configuration"
    ADD CONSTRAINT tenant_configuration_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.tenant_settings
    ADD CONSTRAINT tenant_settings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.change_ledger
    ADD CONSTRAINT uniq_change_ledger_threads UNIQUE (loan_number, flow_name, event_type, thread_id);

--
-- Name: task_tree unqiue_task_tree; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_tree
    ADD CONSTRAINT unqiue_task_tree UNIQUE (qualifier, levels);


--
-- Name: user_config user_config_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_config
    ADD CONSTRAINT user_config_pkey PRIMARY KEY (user_config_id);

ALTER TABLE ONLY public.change_ledger
    ADD CONSTRAINT fk162467s6xrogqlsg49osaf54r FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: tenant_settings fk21wedcc05b1hcym445fio4g2l; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_settings
    ADD CONSTRAINT fk21wedcc05b1hcym445fio4g2l FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);
--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


-- Add unique constraint to tenant_settings table
ALTER TABLE tenant_settings
ADD CONSTRAINT unique_category_strategy_tenant_id UNIQUE (category, strategy, tenant_id);

ALTER TABLE "Tenant"  alter column id type bigint;
ALTER TABLE "change_ledger"  alter column tenant_id type bigint;
ALTER TABLE "tenant_settings"  alter column tenant_id type bigint;
ALTER TABLE "document_auto_result"  alter column tenant_id type bigint;
ALTER TABLE "approval_disclosure"  alter column tenant_id type bigint;
ALTER TABLE "user_info"  alter column tenant_id type bigint;

INSERT INTO tenant_settings (tenant_id, category, strategy, meta)
SELECT
    id,
    'email',
    'rx',
    '{
       "topicRecipients": {
         "PACKAGE_SPLIT": {
           "to": ["Sathya.Selvi@brimmatech.com"],
           "cc": [],
           "bcc": []
         },
         "EXCEPTION_NOTIFICATION": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_FEE_UPDATE": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_INVALID_PASSWORD": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_PROGRESS_NOTIFICATION": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_SITE_NOT_REACHABLE": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "BRIMMA_GLOBAL_SUPPORT": {
           "to": ["saro@brimmatech.com", "gokulp@brimmatech.com"],
           "cc": [""],
           "bcc": [""]
         }
       }
     }'
FROM "Tenant"
WHERE tenant_name = '__vdx__';

CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    task_sequence int NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMP,
    last_updated_at TIMESTAMP,
    delivered_at TIMESTAMP,
    request_id VARCHAR
);

CREATE TABLE user_filter_preferences (
    id BIGSERIAL PRIMARY KEY,
    lo_email VARCHAR(255) NOT NULL UNIQUE,
    user_preference_details JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

alter table approval_disclosure add column loan_folder VARCHAR(255);

ALTER TABLE public."Tenant" add column IF NOT EXISTS send_loan_summary boolean DEFAULT FALSE;

alter table approval_disclosure add column flow_name VARCHAR(255);

ALTER TABLE public."Tenant" add column IF NOT EXISTS send_loan_summary boolean DEFAULT FALSE;

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
