--
-- PostgreSQL database dump
--

-- Dumped from database version 17.2
-- Dumped by pg_dump version 17.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: test; Type: SCHEMA; Schema: -; Owner: postgres
--

ALTER SCHEMA test OWNER TO postgres;

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
-- Name: fair_share_tasks(); Type: PROCEDURE; Schema: test; Owner: postgres
--

CREATE PROCEDURE test.fair_share_tasks()
    LANGUAGE plpgsql
    AS $$
begin
	insert into task (topic, identifier, input) select (array_sample(ARRAY['SPLIT_PDF', 'EXTRACT_DOC_TYPE'], 2))[1], '4', '{"tenantId": 5 }' from generate_series(1,10)  ;
	assert (select count(*) from task where (convert_to_jsonb(input) #>> '{tenantId}')::int =5) =2, 'asda';   
	rollback;
end;
$$;


ALTER PROCEDURE test.fair_share_tasks() OWNER TO postgres;

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


--
-- Name: Classifier_Vallia_Field_Mappings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Classifier_Vallia_Field_Mappings" (
    id integer NOT NULL,
    classifier_field_id integer NOT NULL,
    vallia_field_id integer NOT NULL
);


ALTER TABLE public."Classifier_Vallia_Field_Mappings" OWNER TO postgres;

--
-- Name: Classifier_Vallia_Field_Mappings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Classifier_Vallia_Field_Mappings" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Classifier_Vallia_Field_Mappings_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Criteria_Options; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Criteria_Options" (
    id uuid NOT NULL,
    criteria_type_id uuid NOT NULL,
    criteria_option_name character varying(255),
    sor_criteria_option_field_value character varying(255),
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Criteria_Options" OWNER TO postgres;

--
-- Name: Criteria_Types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Criteria_Types" (
    id uuid NOT NULL,
    criteria_name character varying(255),
    sor_field_id character varying(255),
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Criteria_Types" OWNER TO postgres;

--
-- Name: Doc_Classifier_Document_Categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Doc_Classifier_Document_Categories" (
    id integer NOT NULL,
    category_name text,
    vallia_doc_category_id integer NOT NULL,
    doc_classifier_id integer NOT NULL,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Doc_Classifier_Document_Categories" OWNER TO postgres;

--
-- Name: Doc_Classifier_Document_Categories_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Doc_Classifier_Document_Categories" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Doc_Classifier_Document_Categories_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Doc_Classifier_Documents; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Doc_Classifier_Documents" (
    id integer NOT NULL,
    document_name text,
    document_category_id integer NOT NULL,
    vallia_document_id integer NOT NULL,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Doc_Classifier_Documents" OWNER TO postgres;

--
-- Name: COLUMN "Doc_Classifier_Documents".created_by; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public."Doc_Classifier_Documents".created_by IS 'asdad';


--
-- Name: Doc_Classifier_Documents_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Doc_Classifier_Documents" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Doc_Classifier_Documents_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Doc_Classifier_Fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Doc_Classifier_Fields" (
    id integer NOT NULL,
    field_name text,
    doc_classifier_document_id integer NOT NULL,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Doc_Classifier_Fields" OWNER TO postgres;

--
-- Name: Doc_Classifier_Fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Doc_Classifier_Fields" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Doc_Classifier_Fields_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Doc_Classifiers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Doc_Classifiers" (
    id integer NOT NULL,
    doc_classifier_name text,
    is_active boolean NOT NULL,
    metadata text,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by character varying(255),
    last_updated_by character varying(255)
);


ALTER TABLE public."Doc_Classifiers" OWNER TO postgres;

--
-- Name: Doc_Classifiers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Doc_Classifiers" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Doc_Classifiers_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Document_Extraction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Document_Extraction" (
    id integer NOT NULL,
    document_type character varying(255),
    loan_number character varying(255),
    extracted_json_data jsonb,
    tenant_id integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(255),
    last_updated_by character varying(255),
    attachment_id character varying(255),
    file_name character varying(255),
    doc_qualifier json DEFAULT '{}'::json,
    source_strategy_id integer,
    is_document_split boolean,
    doc_process_id integer,
    page_number character varying(255),
    document_confidence numeric,
    confidence_lines jsonb
);


ALTER TABLE public."Document_Extraction" OWNER TO postgres;

--
-- Name: Document_Processing_Results; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Document_Processing_Results" (
    id integer NOT NULL,
    operation_location character varying(255),
    status character varying(255),
    tenant_id integer NOT NULL,
    blob_folder_name character varying(255),
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    total_pages integer NOT NULL,
    loan_number character varying(255),
    document_name character varying(255),
    classified_page_count integer,
    unclassified_page_count integer,
    attachment_id character varying(255),
    extracted_json_data json,
    no_splits integer,
    metadata_id bigint,
    loan_id character varying(255),
    document_id character varying,
    unclassified_low_confidence_count integer,
    unclassified_tenant_config_missing_count integer,
    document_splits jsonb
);


ALTER TABLE public."Document_Processing_Results" OWNER TO postgres;

--
-- Name: Document_Processing_Results_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Document_Processing_Results" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Document_Processing_Results_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Document_Upload_Failure_Status; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Document_Upload_Failure_Status" (
    id integer NOT NULL,
    tenant_id integer NOT NULL,
    loan_guid uuid NOT NULL,
    document_id integer,
    document_title character varying(255),
    force_create_new_document boolean NOT NULL,
    attachment_title character varying(255),
    attachment_byte_array_data bytea,
    content_type character varying(255),
    attachment_title_with_file_extension character varying(255),
    status character varying(255),
    error_message character varying(255),
    date_time timestamp with time zone NOT NULL
);


ALTER TABLE public."Document_Upload_Failure_Status" OWNER TO postgres;

--
-- Name: Document_Upload_Failure_Status_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Document_Upload_Failure_Status" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Document_Upload_Failure_Status_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Field_SOT_Mappings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Field_SOT_Mappings" (
    id integer NOT NULL,
    vallia_field_id integer NOT NULL,
    is_vallia_doc_sot boolean NOT NULL,
    sor_field_id integer,
    vallia_document_id integer,
    "Tenant_Id" integer NOT NULL
);


ALTER TABLE public."Field_SOT_Mappings" OWNER TO postgres;

--
-- Name: Field_SOT_Mappings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Field_SOT_Mappings" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Field_SOT_Mappings_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Flow_Batch; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Flow_Batch" (
    id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    status text
);


ALTER TABLE public."Flow_Batch" OWNER TO postgres;

--
-- Name: Flow_Batch_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Flow_Batch" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Flow_Batch_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Global_Flow_Flow_Rules_Mappings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Global_Flow_Flow_Rules_Mappings" (
    flow_id uuid NOT NULL,
    flow_rule_id uuid NOT NULL,
    is_active boolean NOT NULL
);


ALTER TABLE public."Global_Flow_Flow_Rules_Mappings" OWNER TO postgres;

--
-- Name: Global_Flow_Rules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Global_Flow_Rules" (
    id uuid NOT NULL,
    flow_rule_name text,
    is_active boolean NOT NULL,
    trigger_event_type text,
    trigger_resource_document_id integer NOT NULL,
    rule_details text,
    rule_actions text,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Global_Flow_Rules" OWNER TO postgres;

--
-- Name: Global_Flows; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Global_Flows" (
    id uuid NOT NULL,
    flow_name text,
    is_active boolean NOT NULL,
    is_default_for_new_tenants boolean NOT NULL,
    loan_milestones text,
    user_roles text,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text,
    is_archived boolean DEFAULT false
);


ALTER TABLE public."Global_Flows" OWNER TO postgres;

--
-- Name: Loan_Batch_Job; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Loan_Batch_Job" (
    id integer NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    status text,
    success_loan_id text
);


ALTER TABLE public."Loan_Batch_Job" OWNER TO postgres;

--
-- Name: Loan_Batch_Job_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Loan_Batch_Job" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Loan_Batch_Job_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Ocrolus_Books; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Ocrolus_Books" (
    id integer NOT NULL,
    tenant_id integer NOT NULL,
    book_uuid text NOT NULL,
    book_name text NOT NULL,
    pk integer NOT NULL,
    created_ts timestamp with time zone NOT NULL,
    book_type text NOT NULL,
    book_class text NOT NULL
);


ALTER TABLE public."Ocrolus_Books" OWNER TO postgres;

--
-- Name: Ocrolus_Books_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Ocrolus_Books" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Ocrolus_Books_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Ocrolus_Documents; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Ocrolus_Documents" (
    id integer NOT NULL,
    docs_uuid text NOT NULL,
    doc_name text NOT NULL,
    page_count integer NOT NULL,
    created_ts timestamp with time zone NOT NULL,
    book_uuid text NOT NULL,
    blob_name text
);


ALTER TABLE public."Ocrolus_Documents" OWNER TO postgres;

--
-- Name: Ocrolus_Documents_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Ocrolus_Documents" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Ocrolus_Documents_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: System_Of_Record_Fields; Type: TABLE; Schema: public; Owner: postgres
--

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
-- Name: System_of_Record_Folders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."System_of_Record_Folders" (
    id integer NOT NULL,
    folder_name character varying(255),
    sor_folder_id character varying(255),
    is_listened_by_vallia_doc_flow boolean NOT NULL,
    tenant_id integer NOT NULL,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by character varying(255),
    last_updated_by character varying(255),
    is_active boolean,
    vallia_document_id integer
);


ALTER TABLE public."System_of_Record_Folders" OWNER TO postgres;

--
-- Name: System_of_Record_Folders_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."System_of_Record_Folders" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."System_of_Record_Folders_id_seq"
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
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_066b15f7e8551737cd1435c5db56ba85" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_066b15f7e8551737cd1435c5db56ba85" OWNER TO postgres;

--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" OWNER TO postgres;

--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" OWNER TO postgres;

--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_20732de50941fcf4857ca9b81c3429e2" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_20732de50941fcf4857ca9b81c3429e2" OWNER TO postgres;

--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" OWNER TO postgres;

--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" OWNER TO postgres;

--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_40bdb70132e8c1706d4825982010fdef" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_40bdb70132e8c1706d4825982010fdef" OWNER TO postgres;

--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_45f7378c55930a4fa766419581bbdb31" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_45f7378c55930a4fa766419581bbdb31" OWNER TO postgres;

--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" OWNER TO postgres;

--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" OWNER TO postgres;

--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_63928464dc2964de4964545de911bbfa" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_63928464dc2964de4964545de911bbfa" OWNER TO postgres;

--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" OWNER TO postgres;

--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" OWNER TO postgres;

--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" OWNER TO postgres;

--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" OWNER TO postgres;

--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_eb7439b5e584e262a89b85853948fe65" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_eb7439b5e584e262a89b85853948fe65" OWNER TO postgres;

--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" OWNER TO postgres;

--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_f61892b9a6831782463551e84ea4fb1f" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_f61892b9a6831782463551e84ea4fb1f" OWNER TO postgres;

--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."TASK_P_fea3894a27c696803b0e286e867f92ae" (
    topic character varying(250) NOT NULL,
    sequence bigint DEFAULT nextval('public.task_sequence_seq'::regclass) NOT NULL,
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
);


ALTER TABLE public."TASK_P_fea3894a27c696803b0e286e867f92ae" OWNER TO postgres;

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

--
-- Name: Tenant_Connections; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Tenant_Connections" (
    connection_tracking_id uuid NOT NULL,
    tenant_id integer NOT NULL,
    connected_at timestamp with time zone NOT NULL,
    connected_by text,
    connection_status text,
    browser text,
    display_resolution text,
    operating_system text,
    encrypted_sor_token text,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Tenant_Connections" OWNER TO postgres;

--
-- Name: Tenant_Flow_Rules; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Tenant_Flow_Rules" (
    id uuid NOT NULL,
    flow_rule_name text,
    is_active boolean NOT NULL,
    trigger_event_type text,
    trigger_resource_document_id integer NOT NULL,
    tenant_flow_id uuid NOT NULL,
    rule_details text,
    rule_actions text,
    global_flow_rule_id uuid NOT NULL,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Tenant_Flow_Rules" OWNER TO postgres;

--
-- Name: TABLE "Tenant_Flow_Rules"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public."Tenant_Flow_Rules" IS 'ads';


--
-- Name: Tenant_Flow_Rules_Activities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Tenant_Flow_Rules_Activities" (
    id uuid NOT NULL,
    flow_rule_activity_name text,
    flow_rule_id uuid NOT NULL,
    document_id text,
    document_name text,
    attachment_id text,
    loan_number text,
    type text,
    status text,
    created_date timestamp with time zone NOT NULL,
    modified_date timestamp with time zone NOT NULL,
    tenant_flow_activity_id uuid NOT NULL,
    ocrolus_doc_uuid text,
    rule_result text,
    blob_folder_name text
);


ALTER TABLE public."Tenant_Flow_Rules_Activities" OWNER TO postgres;

--
-- Name: Tenant_Flows; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Tenant_Flows" (
    id uuid NOT NULL,
    tenant_id integer NOT NULL,
    global_flow_id uuid NOT NULL,
    flow_name text,
    is_active boolean NOT NULL,
    loan_milestones text,
    user_roles text,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text,
    is_archived boolean DEFAULT false,
    is_global_flow boolean NOT NULL
);


ALTER TABLE public."Tenant_Flows" OWNER TO postgres;

--
-- Name: Tenant_Flows_Activities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Tenant_Flows_Activities" (
    id uuid NOT NULL,
    tenant_flow_id uuid NOT NULL,
    loan_number text,
    status text,
    start_date timestamp with time zone NOT NULL,
    completed_date timestamp with time zone,
    borrower_name text
);


ALTER TABLE public."Tenant_Flows_Activities" OWNER TO postgres;

--
-- Name: Tenant_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Tenant" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Tenant_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Vallia_Document_Categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Vallia_Document_Categories" (
    id bigint NOT NULL,
    category_name character varying(255),
    is_all_documents_mapped boolean NOT NULL,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by character varying(255),
    last_updated_by character varying(255)
);


ALTER TABLE public."Vallia_Document_Categories" OWNER TO postgres;

--
-- Name: Vallia_Document_Categories_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Vallia_Document_Categories" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Vallia_Document_Categories_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Vallia_Documents; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Vallia_Documents" (
    id integer NOT NULL,
    document_name character varying(255),
    document_category_id bigint NOT NULL,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by character varying(255),
    last_updated_by character varying(255)
);


ALTER TABLE public."Vallia_Documents" OWNER TO postgres;

--
-- Name: Vallia_Documents_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Vallia_Documents" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Vallia_Documents_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Vallia_Fields; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Vallia_Fields" (
    id integer NOT NULL,
    field_name text,
    data_type text,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone,
    created_by text,
    last_updated_by text
);


ALTER TABLE public."Vallia_Fields" OWNER TO postgres;

--
-- Name: Vallia_Fields_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Vallia_Fields" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Vallia_Fields_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Vallia_Sor_Document_Mappings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Vallia_Sor_Document_Mappings" (
    id integer NOT NULL,
    vallia_document_id integer NOT NULL,
    sor_folder_id integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


ALTER TABLE public."Vallia_Sor_Document_Mappings" OWNER TO postgres;

--
-- Name: Vallia_Sor_Document_Mappings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Vallia_Sor_Document_Mappings" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Vallia_Sor_Document_Mappings_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: Vallia_Sor_Field_Mappings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."Vallia_Sor_Field_Mappings" (
    id integer NOT NULL,
    sor_field_id integer NOT NULL,
    vallia_field_id integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


ALTER TABLE public."Vallia_Sor_Field_Mappings" OWNER TO postgres;

--
-- Name: Vallia_Sor_Field_Mappings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public."Vallia_Sor_Field_Mappings" ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public."Vallia_Sor_Field_Mappings_id_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: __EFMigrationsHistory; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public."__EFMigrationsHistory" (
    "MigrationId" character varying(150) NOT NULL,
    "ProductVersion" character varying(32) NOT NULL
);


ALTER TABLE public."__EFMigrationsHistory" OWNER TO postgres;

--
-- Name: action_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.action_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.action_seq OWNER TO postgres;

--
-- Name: application_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.application_users (
    application_user_id uuid NOT NULL,
    email character varying(255),
    first_name character varying(255),
    is_active boolean,
    last_name character varying(255),
    org_city character varying(255),
    org_state character varying(255),
    org_street1 character varying(255),
    org_street2 character varying(255),
    org_zip character varying(255),
    phone character varying(255),
    client_id uuid,
    role_id uuid
);


ALTER TABLE public.application_users OWNER TO postgres;

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


--
-- Name: classification_fallback_providers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.classification_fallback_providers (
    id integer NOT NULL,
    endpoint character varying(255),
    model_id character varying(255),
    key character varying(255),
    ranking integer
);


ALTER TABLE public.classification_fallback_providers OWNER TO postgres;

--
-- Name: classification_fallback_providers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.classification_fallback_providers_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.classification_fallback_providers_id_seq OWNER TO postgres;

--
-- Name: classification_fallback_providers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.classification_fallback_providers_id_seq OWNED BY public.classification_fallback_providers.id;


--
-- Name: classified_documents_results; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.classified_documents_results (
    id integer NOT NULL,
    classified_document_name character varying(255) NOT NULL,
    confidence double precision NOT NULL,
    created_time timestamp without time zone,
    document_processing_result_id integer NOT NULL,
    is_document_configured boolean
);


ALTER TABLE public.classified_documents_results OWNER TO postgres;

--
-- Name: classified_documents_results_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.classified_documents_results_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.classified_documents_results_id_seq OWNER TO postgres;

--
-- Name: classified_documents_results_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.classified_documents_results_id_seq OWNED BY public.classified_documents_results.id;


--
-- Name: client; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.client (
    client_id uuid NOT NULL,
    address_line1 character varying(255),
    address_line2 character varying(255),
    admin_comments character varying(255),
    city character varying(255),
    client_name character varying(255),
    company_name character varying(255),
    created_at timestamp without time zone,
    created_by character varying(255),
    currency character varying(255),
    email character varying(255),
    is_active boolean,
    last_updated_at timestamp without time zone,
    last_updated_by character varying(255),
    phone_number character varying(255),
    state character varying(255),
    zipcode character varying(255)
);


ALTER TABLE public.client OWNER TO postgres;

--
-- Name: client_plan; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.client_plan (
    client_plan_id uuid NOT NULL,
    validity_end_date timestamp without time zone,
    validity_start_date timestamp without time zone,
    created_at timestamp without time zone,
    created_by character varying(255),
    is_active boolean,
    last_updated_at timestamp without time zone,
    last_updated_by character varying(255),
    client_id uuid,
    plan_id uuid
);


ALTER TABLE public.client_plan OWNER TO postgres;

--
-- Name: config_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config_roles (
    role_id uuid NOT NULL,
    is_active boolean NOT NULL,
    role_name character varying(255)
);


ALTER TABLE public.config_roles OWNER TO postgres;

--
-- Name: TABLE config_roles; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.config_roles IS 'asdad';


--
-- Name: content_understanding_analyzers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.content_understanding_analyzers (
    document_type character varying(255),
    analyzer_id character varying(255),
    id integer NOT NULL,
    profile character varying(255)
);


ALTER TABLE public.content_understanding_analyzers OWNER TO postgres;

--
-- Name: content_understanding_analyzers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.content_understanding_analyzers_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.content_understanding_analyzers_id_seq OWNER TO postgres;

--
-- Name: content_understanding_analyzers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.content_understanding_analyzers_id_seq OWNED BY public.content_understanding_analyzers.id;


--
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


ALTER TABLE public.databasechangelog OWNER TO postgres;

--
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


ALTER TABLE public.databasechangeloglock OWNER TO postgres;

--
-- Name: deferred_events; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.deferred_events (
    jparepositorystate_id bigint NOT NULL,
    deferredevents character varying(255)
);


ALTER TABLE public.deferred_events OWNER TO postgres;

--
-- Name: document_auto_result; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.document_auto_result (
    id character varying(32) NOT NULL,
    loan_number character varying(255),
    document_name character varying(255),
    document_type character varying(255),
    update_to_los boolean,
    upload_to_los boolean,
    doc_file_path character varying(1024),
    file_content bytea,
    control_mode character varying(255),
    created_date timestamp with time zone,
    tenant_id bigint,
    doc_request text,
    investor_name character varying(255)
);


ALTER TABLE public.document_auto_result OWNER TO postgres;

--
-- Name: document_extraction_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.document_extraction_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.document_extraction_id_seq OWNER TO postgres;

--
-- Name: document_extraction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.document_extraction_id_seq OWNED BY public."Document_Extraction".id;


--
-- Name: document_metadata; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.document_metadata (
    id bigint NOT NULL,
    blob_storage_url text,
    document_check_sum character varying(255),
    document_id character varying(255),
    document_name character varying(255),
    file_content bytea,
    file_name character varying(255),
    tenant_id integer,
    run_id character varying(255),
    attachment_id character varying(255),
    change_ledger_id integer,
    created_at timestamp with time zone,
    loan_number_details jsonb,
    control_mode character varying(255),
    user_id uuid,
    investor_name character varying(255)
);


ALTER TABLE public.document_metadata OWNER TO postgres;

--
-- Name: document_metadata_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.document_metadata_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.document_metadata_id_seq OWNER TO postgres;

--
-- Name: document_metadata_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.document_metadata_id_seq OWNED BY public.document_metadata.id;


--
-- Name: document_model; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.document_model (
    id integer NOT NULL,
    document_type character varying(255),
    model_id character varying(255)
);


ALTER TABLE public.document_model OWNER TO postgres;

--
-- Name: document_model_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.document_model_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.document_model_id_seq OWNER TO postgres;

--
-- Name: document_model_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.document_model_id_seq OWNED BY public.document_model.id;


--
-- Name: document_upload_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.document_upload_logs (
    id integer NOT NULL,
    full_name character varying(255),
    email_address character varying(255),
    organisation character varying(255),
    operation_location character varying(255),
    blob_folder_name character varying(255),
    document_name character varying(255),
    status character varying(50),
    start_time timestamp with time zone,
    end_time timestamp with time zone
);


ALTER TABLE public.document_upload_logs OWNER TO postgres;

--
-- Name: document_upload_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.document_upload_logs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.document_upload_logs_id_seq OWNER TO postgres;

--
-- Name: document_upload_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.document_upload_logs_id_seq OWNED BY public.document_upload_logs.id;


--
-- Name: econsent_tracker; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.econsent_tracker (
    track_id bigint NOT NULL,
    created_date timestamp(6) without time zone,
    error_message text,
    last_updated_date timestamp(6) without time zone,
    loan_number character varying(255),
    status character varying(255),
    change_ledger_id integer,
    tenant_id integer
);


ALTER TABLE public.econsent_tracker OWNER TO postgres;

--
-- Name: econsent_tracker_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.econsent_tracker_seq
    AS integer
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.econsent_tracker_seq OWNER TO postgres;

--
-- Name: extraction_hubs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.extraction_hubs (
    id integer NOT NULL,
    document_type text,
    analyzer_id text,
    hub_id text,
    field_schema jsonb
);


ALTER TABLE public.extraction_hubs OWNER TO postgres;

--
-- Name: extraction_hubs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.extraction_hubs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.extraction_hubs_id_seq OWNER TO postgres;

--
-- Name: extraction_hubs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.extraction_hubs_id_seq OWNED BY public.extraction_hubs.id;


--
-- Name: feature; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.feature (
    feature_id uuid NOT NULL,
    created_at timestamp without time zone,
    created_by character varying(255),
    feature_description character varying(255),
    feature_name character varying(255),
    is_active boolean,
    is_count_specific boolean,
    last_updated_at timestamp without time zone,
    last_updated_by character varying(255),
    application_id uuid
);


ALTER TABLE public.feature OWNER TO postgres;

--
-- Name: feature_config; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.feature_config (
    feature_config_id uuid NOT NULL,
    feature_name character varying(255),
    fields text,
    application_id uuid
);


ALTER TABLE public.feature_config OWNER TO postgres;

--
-- Name: feature_role_map_details; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.feature_role_map_details (
    is_mapped boolean NOT NULL,
    client_id uuid NOT NULL,
    application_id uuid NOT NULL,
    feature_id uuid NOT NULL,
    role_id uuid NOT NULL,
    ismapped boolean NOT NULL
);


ALTER TABLE public.feature_role_map_details OWNER TO postgres;

--
-- Name: guard_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.guard_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.guard_seq OWNER TO postgres;

--
-- Name: hooks; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.hooks (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    endpoint character varying(255) NOT NULL,
    tenant_id integer NOT NULL,
    hook_type character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.hooks OWNER TO postgres;

--
-- Name: COLUMN hooks.hook_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.hooks.hook_type IS 'asd';


--
-- Name: hooks_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.hooks_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.hooks_id_seq OWNER TO postgres;

--
-- Name: hooks_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.hooks_id_seq OWNED BY public.hooks.id;


--
-- Name: investors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.investors (
    investor_id bigint NOT NULL,
    investor_name character varying(255) NOT NULL,
    is_purchase_advice_applicable boolean,
    is_purchase_suspension_applicable boolean,
    is_purchase_advice_active boolean,
    is_purchase_suspension_active boolean,
    meta_data character varying(255),
    tenant_id bigint
);


ALTER TABLE public.investors OWNER TO postgres;

--
-- Name: investors_investor_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.investors_investor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.investors_investor_id_seq OWNER TO postgres;

--
-- Name: investors_investor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.investors_investor_id_seq OWNED BY public.investors.investor_id;


--
-- Name: plan; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.plan (
    plan_id uuid NOT NULL,
    cost_price real,
    created_at timestamp without time zone,
    created_by character varying(255),
    duration_in_months integer,
    is_active boolean,
    is_custom_plan boolean,
    last_updated_at timestamp without time zone,
    last_updated_by character varying(255),
    licences_count integer,
    plan_description character varying(255),
    plan_name character varying(255),
    application_id uuid
);


ALTER TABLE public.plan OWNER TO postgres;

--
-- Name: rule_audit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rule_audit (
    id integer NOT NULL,
    loan_number character varying(255),
    modified_at timestamp(6) with time zone,
    modified_by character varying(255),
    modified_rule_data json,
    rule_batch_id integer,
    tenant_id integer
);


ALTER TABLE public.rule_audit OWNER TO postgres;

--
-- Name: rule_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.rule_audit ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.rule_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: rule_batch; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rule_batch (
    id integer NOT NULL,
    loan_time_stamp timestamp without time zone,
    tenant_id integer,
    loan_number character varying(255),
    last_updated_at timestamp without time zone DEFAULT now(),
    created_at timestamp without time zone DEFAULT now(),
    current_milestone character varying(255),
    flow_name character varying(255),
    loan_users json,
    status character varying(255),
    milestone character varying(255),
    folder_name character varying(255),
    is_reviewed boolean DEFAULT false
);


ALTER TABLE public.rule_batch OWNER TO postgres;

--
-- Name: rule_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.rule_batch_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rule_batch_id_seq OWNER TO postgres;

--
-- Name: rule_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.rule_batch_id_seq OWNED BY public.rule_batch.id;


--
-- Name: rule_entity; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rule_entity (
    id integer NOT NULL,
    entity_type character varying(255) NOT NULL,
    extracted_json json,
    doc_qualifier json,
    checksum character varying(255),
    extracted_json_checksum character varying(255),
    doc_extraction_id integer,
    batch_id integer,
    document_confidence numeric(38,2)
);


ALTER TABLE public.rule_entity OWNER TO postgres;

--
-- Name: rule_entity_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.rule_entity_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rule_entity_id_seq OWNER TO postgres;

--
-- Name: rule_entity_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.rule_entity_id_seq OWNED BY public.rule_entity.id;


--
-- Name: rule_result; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.rule_result (
    id integer NOT NULL,
    tenant_rule_id integer NOT NULL,
    rule_batch_id integer NOT NULL,
    run_status character varying(255),
    rule_result boolean NOT NULL,
    rule_output json
);


ALTER TABLE public.rule_result OWNER TO postgres;

--
-- Name: rule_result_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.rule_result_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rule_result_id_seq OWNER TO postgres;

--
-- Name: rule_result_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.rule_result_id_seq OWNED BY public.rule_result.id;


--
-- Name: state_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.state_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.state_seq OWNER TO postgres;

--
-- Name: task_activation; Type: TABLE; Schema: public; Owner: postgres
--

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
-- Name: tenant_rule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tenant_rule (
    id integer NOT NULL,
    tenant_id integer,
    rule_id character varying(255),
    is_active boolean,
    flow_names text
);


ALTER TABLE public.tenant_rule OWNER TO postgres;

--
-- Name: tenant_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tenant_rule_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tenant_rule_id_seq OWNER TO postgres;

--
-- Name: tenant_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tenant_rule_id_seq OWNED BY public.tenant_rule.id;


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

CREATE TABLE public.title_order (
    id bigint NOT NULL,
    tenant_id integer,
    order_id text NOT NULL,
    order_type text NOT NULL,
    fees_details text,
    order_detail text,
    created_at timestamp with time zone DEFAULT now(),
    order_status text,
    sync_status text,
    genie_response text,
    retry_log_entries jsonb,
    genie_request text
);


ALTER TABLE public.title_order OWNER TO postgres;

--
-- Name: title_order_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.title_order_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.title_order_id_seq OWNER TO postgres;

--
-- Name: title_order_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.title_order_id_seq OWNED BY public.title_order.id;


--
-- Name: transition_seq; Type: SEQUENCE; Schema: public; Owner: postgres
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
-- Name: user_preference; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_preference (
    id integer NOT NULL,
    user_id uuid NOT NULL,
    user_preference_details jsonb,
    created_at timestamp with time zone,
    last_updated_at timestamp with time zone
);


ALTER TABLE public.user_preference OWNER TO postgres;

--
-- Name: user_preference_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_preference_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_preference_id_seq OWNER TO postgres;

--
-- Name: user_preference_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_preference_id_seq OWNED BY public.user_preference.id;


--
-- Name: vallia_application; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vallia_application (
    application_id uuid NOT NULL,
    application_description character varying(255),
    application_name character varying(255),
    created_at timestamp without time zone,
    created_by character varying(255),
    is_active boolean,
    last_updated_at timestamp without time zone,
    last_updated_by character varying(255)
);


ALTER TABLE public.vallia_application OWNER TO postgres;

--
-- Name: vallia_assist; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vallia_assist (
    id bigint NOT NULL,
    process_type character varying(255),
    vector_id character varying(255),
    thread_id character varying(255),
    loan_id character varying(255),
    borrower_name character varying(255),
    tenant_id integer,
    created_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.vallia_assist OWNER TO postgres;

--
-- Name: vallia_assist_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.vallia_assist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.vallia_assist_id_seq OWNER TO postgres;

--
-- Name: vallia_assist_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.vallia_assist_id_seq OWNED BY public.vallia_assist.id;


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba85" FOR VALUES IN ('INTERNAL_PUSH_HOOKS');


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" FOR VALUES IN ('NOOP_SUPERVISOR');


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" FOR VALUES IN ('LOS_DOCUMENT_UPLOAD');


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429e2" FOR VALUES IN ('SPLIT_PDF');


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" FOR VALUES IN ('LOS_ADD_LOAN_ENHANCED_CONDITION');


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" FOR VALUES IN ('UPLOAD_DOCUMENT_FOR_CX_EX');


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fdef" FOR VALUES IN ('INVOKE_HTTP');


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb31" FOR VALUES IN ('SYNC_WITH_GENIE');


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" FOR VALUES IN ('END_RUN');


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" FOR VALUES IN ('RUN_RULES');


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bbfa" FOR VALUES IN ('UPDATE_ENCOMPASS');


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" FOR VALUES IN ('INTERNAL_HTTP');


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" FOR VALUES IN ('LOS_LOAN_UPDATE');


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" FOR VALUES IN ('foo');


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" FOR VALUES IN ('CLASSIFY_PDF');


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe65" FOR VALUES IN ('EXTRACT_DOC_TYPE');


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" FOR VALUES IN ('INTERNAL_SLEEP');


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb1f" FOR VALUES IN ('ASK_AND_ANALYZE');


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae; Type: TABLE ATTACH; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92ae" FOR VALUES IN ('DOWNLOAD_ATTACHMENT');


--
-- Name: Document_Extraction id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Extraction" ALTER COLUMN id SET DEFAULT nextval('public.document_extraction_id_seq'::regclass);


--
-- Name: Tenant_Configuration id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Configuration" ALTER COLUMN id SET DEFAULT nextval('public.tenant_configuration_id_seq'::regclass);


--
-- Name: change_ledger id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.change_ledger ALTER COLUMN id SET DEFAULT nextval('public.change_ledger_id_seq'::regclass);


--
-- Name: classification_fallback_providers id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classification_fallback_providers ALTER COLUMN id SET DEFAULT nextval('public.classification_fallback_providers_id_seq'::regclass);


--
-- Name: classified_documents_results id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classified_documents_results ALTER COLUMN id SET DEFAULT nextval('public.classified_documents_results_id_seq'::regclass);


--
-- Name: content_understanding_analyzers id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.content_understanding_analyzers ALTER COLUMN id SET DEFAULT nextval('public.content_understanding_analyzers_id_seq'::regclass);


--
-- Name: document_metadata id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_metadata ALTER COLUMN id SET DEFAULT nextval('public.document_metadata_id_seq'::regclass);


--
-- Name: document_model id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_model ALTER COLUMN id SET DEFAULT nextval('public.document_model_id_seq'::regclass);


--
-- Name: document_upload_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_upload_logs ALTER COLUMN id SET DEFAULT nextval('public.document_upload_logs_id_seq'::regclass);


--
-- Name: extraction_hubs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.extraction_hubs ALTER COLUMN id SET DEFAULT nextval('public.extraction_hubs_id_seq'::regclass);


--
-- Name: investors investor_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.investors ALTER COLUMN investor_id SET DEFAULT nextval('public.investors_investor_id_seq'::regclass);


--
-- Name: rule_batch id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_batch ALTER COLUMN id SET DEFAULT nextval('public.rule_batch_id_seq'::regclass);


--
-- Name: rule_entity id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_entity ALTER COLUMN id SET DEFAULT nextval('public.rule_entity_id_seq'::regclass);


--
-- Name: rule_result id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_result ALTER COLUMN id SET DEFAULT nextval('public.rule_result_id_seq'::regclass);


--
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
-- Name: tenant_rule id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_rule ALTER COLUMN id SET DEFAULT nextval('public.tenant_rule_id_seq'::regclass);


--
-- Name: tenant_settings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_settings ALTER COLUMN id SET DEFAULT nextval('public.tenant_settings_id_seq'::regclass);


--
-- Name: title_order id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.title_order ALTER COLUMN id SET DEFAULT nextval('public.title_order_id_seq'::regclass);


--
-- Name: user_preference id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preference ALTER COLUMN id SET DEFAULT nextval('public.user_preference_id_seq'::regclass);


--
-- Name: vallia_assist id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vallia_assist ALTER COLUMN id SET DEFAULT nextval('public.vallia_assist_id_seq'::regclass);


--
-- Name: Ocrolus_Books AK_Ocrolus_Books_book_uuid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Ocrolus_Books"
    ADD CONSTRAINT "AK_Ocrolus_Books_book_uuid" UNIQUE (book_uuid);


--
-- Name: hooks FK_unique_hook_registrations; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.hooks
    ADD CONSTRAINT "FK_unique_hook_registrations" UNIQUE (tenant_id, hook_type);


--
-- Name: Application_Config PK_Application_Config; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Application_Config"
    ADD CONSTRAINT "PK_Application_Config" PRIMARY KEY (id);


--
-- Name: Classifier_Vallia_Field_Mappings PK_Classifier_Vallia_Field_Mappings; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Classifier_Vallia_Field_Mappings"
    ADD CONSTRAINT "PK_Classifier_Vallia_Field_Mappings" PRIMARY KEY (id);


--
-- Name: Criteria_Options PK_Criteria_Options; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Criteria_Options"
    ADD CONSTRAINT "PK_Criteria_Options" PRIMARY KEY (id);


--
-- Name: Criteria_Types PK_Criteria_Types; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Criteria_Types"
    ADD CONSTRAINT "PK_Criteria_Types" PRIMARY KEY (id);


--
-- Name: Doc_Classifier_Document_Categories PK_Doc_Classifier_Document_Categories; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Document_Categories"
    ADD CONSTRAINT "PK_Doc_Classifier_Document_Categories" PRIMARY KEY (id);


--
-- Name: Doc_Classifier_Documents PK_Doc_Classifier_Documents; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Documents"
    ADD CONSTRAINT "PK_Doc_Classifier_Documents" PRIMARY KEY (id);


--
-- Name: Doc_Classifier_Fields PK_Doc_Classifier_Fields; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Fields"
    ADD CONSTRAINT "PK_Doc_Classifier_Fields" PRIMARY KEY (id);


--
-- Name: Doc_Classifiers PK_Doc_Classifiers; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifiers"
    ADD CONSTRAINT "PK_Doc_Classifiers" PRIMARY KEY (id);


--
-- Name: Document_Processing_Results PK_Document_Processing_Results; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Processing_Results"
    ADD CONSTRAINT "PK_Document_Processing_Results" PRIMARY KEY (id);


--
-- Name: Document_Upload_Failure_Status PK_Document_Upload_Failure_Status; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Upload_Failure_Status"
    ADD CONSTRAINT "PK_Document_Upload_Failure_Status" PRIMARY KEY (id);


--
-- Name: Field_SOT_Mappings PK_Field_SOT_Mappings; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Field_SOT_Mappings"
    ADD CONSTRAINT "PK_Field_SOT_Mappings" PRIMARY KEY (id);


--
-- Name: Flow_Batch PK_Flow_Batch; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Flow_Batch"
    ADD CONSTRAINT "PK_Flow_Batch" PRIMARY KEY (id);


--
-- Name: Global_Flow_Flow_Rules_Mappings PK_Global_Flow_Flow_Rules_Mappings; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Global_Flow_Flow_Rules_Mappings"
    ADD CONSTRAINT "PK_Global_Flow_Flow_Rules_Mappings" PRIMARY KEY (flow_id, flow_rule_id);


--
-- Name: Global_Flow_Rules PK_Global_Flow_Rules; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Global_Flow_Rules"
    ADD CONSTRAINT "PK_Global_Flow_Rules" PRIMARY KEY (id);


--
-- Name: Global_Flows PK_Global_Flows; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Global_Flows"
    ADD CONSTRAINT "PK_Global_Flows" PRIMARY KEY (id);


--
-- Name: Loan_Batch_Job PK_Loan_Batch_Job; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Loan_Batch_Job"
    ADD CONSTRAINT "PK_Loan_Batch_Job" PRIMARY KEY (id);


--
-- Name: Ocrolus_Books PK_Ocrolus_Books; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Ocrolus_Books"
    ADD CONSTRAINT "PK_Ocrolus_Books" PRIMARY KEY (id);


--
-- Name: Ocrolus_Documents PK_Ocrolus_Documents; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Ocrolus_Documents"
    ADD CONSTRAINT "PK_Ocrolus_Documents" PRIMARY KEY (id);


--
-- Name: System_Of_Record_Fields PK_System_Of_Record_Fields; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_Of_Record_Fields"
    ADD CONSTRAINT "PK_System_Of_Record_Fields" PRIMARY KEY (id);


--
-- Name: System_Of_Records PK_System_Of_Records; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_Of_Records"
    ADD CONSTRAINT "PK_System_Of_Records" PRIMARY KEY (id);


--
-- Name: System_of_Record_Folders PK_System_of_Record_Folders; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_of_Record_Folders"
    ADD CONSTRAINT "PK_System_of_Record_Folders" PRIMARY KEY (id);


--
-- Name: Tenant PK_Tenant; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant"
    ADD CONSTRAINT "PK_Tenant" PRIMARY KEY (id);


--
-- Name: Tenant_Connections PK_Tenant_Connections; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Connections"
    ADD CONSTRAINT "PK_Tenant_Connections" PRIMARY KEY (connection_tracking_id);


--
-- Name: Tenant_Flow_Rules PK_Tenant_Flow_Rules; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flow_Rules"
    ADD CONSTRAINT "PK_Tenant_Flow_Rules" PRIMARY KEY (id);


--
-- Name: Tenant_Flow_Rules_Activities PK_Tenant_Flow_Rules_Activities; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flow_Rules_Activities"
    ADD CONSTRAINT "PK_Tenant_Flow_Rules_Activities" PRIMARY KEY (id);


--
-- Name: Tenant_Flows PK_Tenant_Flows; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flows"
    ADD CONSTRAINT "PK_Tenant_Flows" PRIMARY KEY (id);


--
-- Name: Tenant_Flows_Activities PK_Tenant_Flows_Activities; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flows_Activities"
    ADD CONSTRAINT "PK_Tenant_Flows_Activities" PRIMARY KEY (id);


--
-- Name: Vallia_Document_Categories PK_Vallia_Document_Categories; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Document_Categories"
    ADD CONSTRAINT "PK_Vallia_Document_Categories" PRIMARY KEY (id);


--
-- Name: Vallia_Documents PK_Vallia_Documents; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Documents"
    ADD CONSTRAINT "PK_Vallia_Documents" PRIMARY KEY (id);


--
-- Name: Vallia_Fields PK_Vallia_Fields; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Fields"
    ADD CONSTRAINT "PK_Vallia_Fields" PRIMARY KEY (id);


--
-- Name: Vallia_Sor_Document_Mappings PK_Vallia_Sor_Document_Mappings; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Sor_Document_Mappings"
    ADD CONSTRAINT "PK_Vallia_Sor_Document_Mappings" PRIMARY KEY (id);


--
-- Name: Vallia_Sor_Field_Mappings PK_Vallia_Sor_Field_Mappings; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Sor_Field_Mappings"
    ADD CONSTRAINT "PK_Vallia_Sor_Field_Mappings" PRIMARY KEY (id);


--
-- Name: __EFMigrationsHistory PK___EFMigrationsHistory; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."__EFMigrationsHistory"
    ADD CONSTRAINT "PK___EFMigrationsHistory" PRIMARY KEY ("MigrationId");


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
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85 TASK_P_066b15f7e8551737cd1435c5db56ba85_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_066b15f7e8551737cd1435c5db56ba85"
    ADD CONSTRAINT "TASK_P_066b15f7e8551737cd1435c5db56ba85_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7 TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7"
    ADD CONSTRAINT "TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad"
    ADD CONSTRAINT "TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2 TASK_P_20732de50941fcf4857ca9b81c3429e2_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_20732de50941fcf4857ca9b81c3429e2"
    ADD CONSTRAINT "TASK_P_20732de50941fcf4857ca9b81c3429e2_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e TASK_P_3902ad9b27d03b0853ff7b2f3084624e_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e"
    ADD CONSTRAINT "TASK_P_3902ad9b27d03b0853ff7b2f3084624e_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157 TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157"
    ADD CONSTRAINT "TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef TASK_P_40bdb70132e8c1706d4825982010fdef_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_40bdb70132e8c1706d4825982010fdef"
    ADD CONSTRAINT "TASK_P_40bdb70132e8c1706d4825982010fdef_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31 TASK_P_45f7378c55930a4fa766419581bbdb31_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_45f7378c55930a4fa766419581bbdb31"
    ADD CONSTRAINT "TASK_P_45f7378c55930a4fa766419581bbdb31_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb TASK_P_488fcfd5aca5830dce22e73168a4aeeb_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb"
    ADD CONSTRAINT "TASK_P_488fcfd5aca5830dce22e73168a4aeeb_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e TASK_P_5bf5fc53e7190396e56324d9b61afb0e_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e"
    ADD CONSTRAINT "TASK_P_5bf5fc53e7190396e56324d9b61afb0e_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa TASK_P_63928464dc2964de4964545de911bbfa_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_63928464dc2964de4964545de911bbfa"
    ADD CONSTRAINT "TASK_P_63928464dc2964de4964545de911bbfa_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54 TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54"
    ADD CONSTRAINT "TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4 TASK_P_79d91bae8fe6a0628d436e816a8615d4_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_79d91bae8fe6a0628d436e816a8615d4"
    ADD CONSTRAINT "TASK_P_79d91bae8fe6a0628d436e816a8615d4_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8 TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8"
    ADD CONSTRAINT "TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8 TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8"
    ADD CONSTRAINT "TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65 TASK_P_eb7439b5e584e262a89b85853948fe65_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_eb7439b5e584e262a89b85853948fe65"
    ADD CONSTRAINT "TASK_P_eb7439b5e584e262a89b85853948fe65_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae"
    ADD CONSTRAINT "TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f TASK_P_f61892b9a6831782463551e84ea4fb1f_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_f61892b9a6831782463551e84ea4fb1f"
    ADD CONSTRAINT "TASK_P_f61892b9a6831782463551e84ea4fb1f_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae TASK_P_fea3894a27c696803b0e286e867f92ae_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_fea3894a27c696803b0e286e867f92ae"
    ADD CONSTRAINT "TASK_P_fea3894a27c696803b0e286e867f92ae_pkey" PRIMARY KEY (topic, sequence);


--
-- Name: task_topic TASK_TOPIC_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_topic
    ADD CONSTRAINT "TASK_TOPIC_pkey" PRIMARY KEY (topic);


--
-- Name: application_users application_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.application_users
    ADD CONSTRAINT application_users_pkey PRIMARY KEY (application_user_id);


--
-- Name: change_ledger change_ledger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.change_ledger
    ADD CONSTRAINT change_ledger_pkey PRIMARY KEY (id);


--
-- Name: classification_fallback_providers classification_fallback_providers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classification_fallback_providers
    ADD CONSTRAINT classification_fallback_providers_pkey PRIMARY KEY (id);


--
-- Name: classified_documents_results classified_documents_results_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classified_documents_results
    ADD CONSTRAINT classified_documents_results_pkey PRIMARY KEY (id);


--
-- Name: client client_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT client_pkey PRIMARY KEY (client_id);


--
-- Name: client_plan client_plan_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.client_plan
    ADD CONSTRAINT client_plan_pkey PRIMARY KEY (client_plan_id);


--
-- Name: config_roles config_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_roles
    ADD CONSTRAINT config_roles_pkey PRIMARY KEY (role_id);


--
-- Name: content_understanding_analyzers content_understanding_analyzers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.content_understanding_analyzers
    ADD CONSTRAINT content_understanding_analyzers_pkey PRIMARY KEY (id);


--
-- Name: content_understanding_analyzers content_understanding_analyzers_uniq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.content_understanding_analyzers
    ADD CONSTRAINT content_understanding_analyzers_uniq UNIQUE NULLS NOT DISTINCT (document_type, analyzer_id, profile);


--
-- Name: databasechangeloglock databasechangeloglock_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);


--
-- Name: document_auto_result document_auto_result_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_auto_result
    ADD CONSTRAINT document_auto_result_pkey PRIMARY KEY (id);


--
-- Name: Document_Extraction document_extraction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Extraction"
    ADD CONSTRAINT document_extraction_pkey PRIMARY KEY (id);


--
-- Name: document_metadata document_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_metadata
    ADD CONSTRAINT document_metadata_pkey PRIMARY KEY (id);


--
-- Name: document_model document_model_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_model
    ADD CONSTRAINT document_model_pkey PRIMARY KEY (id);


--
-- Name: econsent_tracker econsent_tracker_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.econsent_tracker
    ADD CONSTRAINT econsent_tracker_pkey PRIMARY KEY (track_id);


--
-- Name: extraction_hubs extraction_hubs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.extraction_hubs
    ADD CONSTRAINT extraction_hubs_pkey PRIMARY KEY (id);


--
-- Name: feature_config feature_config_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature_config
    ADD CONSTRAINT feature_config_pkey PRIMARY KEY (feature_config_id);


--
-- Name: feature feature_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature
    ADD CONSTRAINT feature_pkey PRIMARY KEY (feature_id);


--
-- Name: feature_role_map_details feature_role_map_details_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature_role_map_details
    ADD CONSTRAINT feature_role_map_details_pkey PRIMARY KEY (application_id, client_id, feature_id, role_id);


--
-- Name: investors investors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.investors
    ADD CONSTRAINT investors_pkey PRIMARY KEY (investor_id);


--
-- Name: plan plan_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan
    ADD CONSTRAINT plan_pkey PRIMARY KEY (plan_id);


--
-- Name: rule_audit rule_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_audit
    ADD CONSTRAINT rule_audit_pkey PRIMARY KEY (id);


--
-- Name: rule_entity rule_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_entity
    ADD CONSTRAINT rule_entity_pkey PRIMARY KEY (id);


--
-- Name: rule_result rule_result_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_result
    ADD CONSTRAINT rule_result_pkey PRIMARY KEY (id);


--
-- Name: rule_batch rule_run_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_batch
    ADD CONSTRAINT rule_run_pkey PRIMARY KEY (id);


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


--
-- Name: tenant_rule tenant_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_rule
    ADD CONSTRAINT tenant_rule_pkey PRIMARY KEY (id);


--
-- Name: tenant_settings tenant_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_settings
    ADD CONSTRAINT tenant_settings_pkey PRIMARY KEY (id);


--
-- Name: tenant_rule tenantruleuniqueidruleid; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_rule
    ADD CONSTRAINT tenantruleuniqueidruleid UNIQUE (tenant_id, rule_id);


--
-- Name: title_order title_order_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.title_order
    ADD CONSTRAINT title_order_pkey PRIMARY KEY (id);


--
-- Name: Document_Processing_Results uk_c7uuw78fnniv47ne56fn5xydx; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Processing_Results"
    ADD CONSTRAINT uk_c7uuw78fnniv47ne56fn5xydx UNIQUE (metadata_id);


--
-- Name: config_roles uk_dkagh6bgpev9qkl6vdwcit218; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config_roles
    ADD CONSTRAINT uk_dkagh6bgpev9qkl6vdwcit218 UNIQUE (role_name);


--
-- Name: rule_audit ukcl2xhe79jmxecmcnm00obhv0s; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_audit
    ADD CONSTRAINT ukcl2xhe79jmxecmcnm00obhv0s UNIQUE (rule_batch_id);


--
-- Name: classification_fallback_providers uniq_cfp_model_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classification_fallback_providers
    ADD CONSTRAINT uniq_cfp_model_id UNIQUE (model_id);


--
-- Name: change_ledger uniq_change_ledger_threads; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.change_ledger
    ADD CONSTRAINT uniq_change_ledger_threads UNIQUE (loan_number, flow_name, event_type, thread_id);


--
-- Name: content_understanding_analyzers uniq_document_analyzer; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.content_understanding_analyzers
    ADD CONSTRAINT uniq_document_analyzer UNIQUE (document_type, analyzer_id);


--
-- Name: document_model unique_name_constraint; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_model
    ADD CONSTRAINT unique_name_constraint UNIQUE (document_type);


--
-- Name: rule_result uniqueruleresults; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_result
    ADD CONSTRAINT uniqueruleresults UNIQUE (tenant_rule_id, rule_batch_id);


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


--
-- Name: user_preference user_preference_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preference
    ADD CONSTRAINT user_preference_pkey PRIMARY KEY (id);


--
-- Name: vallia_application vallia_application_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vallia_application
    ADD CONSTRAINT vallia_application_pkey PRIMARY KEY (application_id);


--
-- Name: vallia_assist vallia_assist_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vallia_assist
    ADD CONSTRAINT vallia_assist_pkey PRIMARY KEY (id);


--
-- Name: IX_Classifier_Vallia_Field_Mappings_classifier_field_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Classifier_Vallia_Field_Mappings_classifier_field_id" ON public."Classifier_Vallia_Field_Mappings" USING btree (classifier_field_id);


--
-- Name: IX_Classifier_Vallia_Field_Mappings_vallia_field_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Classifier_Vallia_Field_Mappings_vallia_field_id" ON public."Classifier_Vallia_Field_Mappings" USING btree (vallia_field_id);


--
-- Name: IX_Criteria_Options_criteria_type_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Criteria_Options_criteria_type_id" ON public."Criteria_Options" USING btree (criteria_type_id);


--
-- Name: IX_Doc_Classifier_Document_Categories_doc_classifier_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Doc_Classifier_Document_Categories_doc_classifier_id" ON public."Doc_Classifier_Document_Categories" USING btree (doc_classifier_id);


--
-- Name: IX_Doc_Classifier_Document_Categories_vallia_doc_category_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Doc_Classifier_Document_Categories_vallia_doc_category_id" ON public."Doc_Classifier_Document_Categories" USING btree (vallia_doc_category_id);


--
-- Name: IX_Doc_Classifier_Documents_document_category_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Doc_Classifier_Documents_document_category_id" ON public."Doc_Classifier_Documents" USING btree (document_category_id);


--
-- Name: IX_Doc_Classifier_Documents_vallia_document_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Doc_Classifier_Documents_vallia_document_id" ON public."Doc_Classifier_Documents" USING btree (vallia_document_id);


--
-- Name: IX_Doc_Classifier_Fields_doc_classifier_document_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Doc_Classifier_Fields_doc_classifier_document_id" ON public."Doc_Classifier_Fields" USING btree (doc_classifier_document_id);


--
-- Name: IX_Document_Upload_Failure_Status_tenant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Document_Upload_Failure_Status_tenant_id" ON public."Document_Upload_Failure_Status" USING btree (tenant_id);


--
-- Name: IX_Field_SOT_Mappings_Tenant_Id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Field_SOT_Mappings_Tenant_Id" ON public."Field_SOT_Mappings" USING btree ("Tenant_Id");


--
-- Name: IX_Field_SOT_Mappings_sor_field_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Field_SOT_Mappings_sor_field_id" ON public."Field_SOT_Mappings" USING btree (sor_field_id);


--
-- Name: IX_Field_SOT_Mappings_vallia_document_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Field_SOT_Mappings_vallia_document_id" ON public."Field_SOT_Mappings" USING btree (vallia_document_id);


--
-- Name: IX_Field_SOT_Mappings_vallia_field_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Field_SOT_Mappings_vallia_field_id" ON public."Field_SOT_Mappings" USING btree (vallia_field_id);


--
-- Name: IX_Global_Flow_Flow_Rules_Mappings_flow_rule_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Global_Flow_Flow_Rules_Mappings_flow_rule_id" ON public."Global_Flow_Flow_Rules_Mappings" USING btree (flow_rule_id);


--
-- Name: IX_Global_Flow_Rules_trigger_resource_document_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Global_Flow_Rules_trigger_resource_document_id" ON public."Global_Flow_Rules" USING btree (trigger_resource_document_id);


--
-- Name: IX_Ocrolus_Books_tenant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Ocrolus_Books_tenant_id" ON public."Ocrolus_Books" USING btree (tenant_id);


--
-- Name: IX_Ocrolus_Documents_book_uuid; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Ocrolus_Documents_book_uuid" ON public."Ocrolus_Documents" USING btree (book_uuid);


--
-- Name: IX_System_Of_Record_Fields_sor_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_System_Of_Record_Fields_sor_id" ON public."System_Of_Record_Fields" USING btree (sor_id);


--
-- Name: IX_System_Of_Record_Fields_tenant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_System_Of_Record_Fields_tenant_id" ON public."System_Of_Record_Fields" USING btree (tenant_id);


--
-- Name: IX_System_of_Record_Folders_tenant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_System_of_Record_Folders_tenant_id" ON public."System_of_Record_Folders" USING btree (tenant_id);


--
-- Name: IX_Tenant_Connections_tenant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Connections_tenant_id" ON public."Tenant_Connections" USING btree (tenant_id);


--
-- Name: IX_Tenant_Flow_Rules_Activities_flow_rule_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flow_Rules_Activities_flow_rule_id" ON public."Tenant_Flow_Rules_Activities" USING btree (flow_rule_id);


--
-- Name: IX_Tenant_Flow_Rules_Activities_tenant_flow_activity_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flow_Rules_Activities_tenant_flow_activity_id" ON public."Tenant_Flow_Rules_Activities" USING btree (tenant_flow_activity_id);


--
-- Name: IX_Tenant_Flow_Rules_global_flow_rule_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flow_Rules_global_flow_rule_id" ON public."Tenant_Flow_Rules" USING btree (global_flow_rule_id);


--
-- Name: IX_Tenant_Flow_Rules_tenant_flow_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flow_Rules_tenant_flow_id" ON public."Tenant_Flow_Rules" USING btree (tenant_flow_id);


--
-- Name: IX_Tenant_Flow_Rules_trigger_resource_document_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flow_Rules_trigger_resource_document_id" ON public."Tenant_Flow_Rules" USING btree (trigger_resource_document_id);


--
-- Name: IX_Tenant_Flows_Activities_tenant_flow_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flows_Activities_tenant_flow_id" ON public."Tenant_Flows_Activities" USING btree (tenant_flow_id);


--
-- Name: IX_Tenant_Flows_global_flow_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flows_global_flow_id" ON public."Tenant_Flows" USING btree (global_flow_id);


--
-- Name: IX_Tenant_Flows_tenant_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_Flows_tenant_id" ON public."Tenant_Flows" USING btree (tenant_id);


--
-- Name: IX_Tenant_doc_classifier_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_doc_classifier_id" ON public."Tenant" USING btree (doc_classifier_id);


--
-- Name: IX_Tenant_sor_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Tenant_sor_id" ON public."Tenant" USING btree (sor_id);


--
-- Name: IX_Vallia_Documents_document_category_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Vallia_Documents_document_category_id" ON public."Vallia_Documents" USING btree (document_category_id);


--
-- Name: IX_Vallia_Sor_Document_Mappings_sor_folder_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Vallia_Sor_Document_Mappings_sor_folder_id" ON public."Vallia_Sor_Document_Mappings" USING btree (sor_folder_id);


--
-- Name: IX_Vallia_Sor_Document_Mappings_vallia_document_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Vallia_Sor_Document_Mappings_vallia_document_id" ON public."Vallia_Sor_Document_Mappings" USING btree (vallia_document_id);


--
-- Name: IX_Vallia_Sor_Field_Mappings_sor_field_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Vallia_Sor_Field_Mappings_sor_field_id" ON public."Vallia_Sor_Field_Mappings" USING btree (sor_field_id);


--
-- Name: IX_Vallia_Sor_Field_Mappings_vallia_field_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "IX_Vallia_Sor_Field_Mappings_vallia_field_id" ON public."Vallia_Sor_Field_Mappings" USING btree (vallia_field_id);


--
-- Name: task_by_id_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_by_id_idx ON ONLY public.task USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_066b15f7e8551737cd1435_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_066b15f7e8551737cd1435_topic_identifier_state_sequen_idx" ON public."TASK_P_066b15f7e8551737cd1435c5db56ba85" USING btree (topic, identifier, state, sequence);


--
-- Name: task_by_creation; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_by_creation ON ONLY public.task USING btree (created, sequence);


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_066b15f7e8551737cd1435c5db56ba85_created_sequence_idx" ON public."TASK_P_066b15f7e8551737cd1435c5db56ba85" USING btree (created, sequence);


--
-- Name: idx_task_input_json; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_task_input_json ON ONLY public.task USING gin (input_json);


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_066b15f7e8551737cd1435c5db56ba85_input_json_idx" ON public."TASK_P_066b15f7e8551737cd1435c5db56ba85" USING gin (input_json);


--
-- Name: task_by_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_by_reference_idx ON ONLY public.task USING btree (reference);


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_066b15f7e8551737cd1435c5db56ba85_reference_idx" ON public."TASK_P_066b15f7e8551737cd1435c5db56ba85" USING btree (reference);


--
-- Name: task_by_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_by_owner_idx ON ONLY public.task USING btree (state, owner);


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_066b15f7e8551737cd1435c5db56ba85_state_owner_idx" ON public."TASK_P_066b15f7e8551737cd1435c5db56ba85" USING btree (state, owner);


--
-- Name: task_by_topic_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_by_topic_idx ON ONLY public.task USING btree (topic, state, sequence);


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba8_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_066b15f7e8551737cd1435c5db56ba8_topic_state_sequence_idx" ON public."TASK_P_066b15f7e8551737cd1435c5db56ba85" USING btree (topic, state, sequence);


--
-- Name: task_by_completion_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_by_completion_idx ON ONLY public.task USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_066b15f7e8551737cd1435c5db56ba_topic_state_completed_idx" ON public."TASK_P_066b15f7e8551737cd1435c5db56ba85" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_0f89e2e28584f2ca1d7b98823544f4_topic_state_completed_idx" ON public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_created_sequence_idx" ON public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" USING btree (created, sequence);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_input_json_idx" ON public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" USING gin (input_json);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_reference_idx" ON public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" USING btree (reference);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_state_owner_idx" ON public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" USING btree (state, owner);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_0f89e2e28584f2ca1d7b98823544f4e_topic_state_sequence_idx" ON public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" USING btree (topic, state, sequence);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_0f89e2e28584f2ca1d7b98_topic_identifier_state_sequen_idx" ON public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_1fcdba9a2e7f9d1311a06138e820e9_topic_state_completed_idx" ON public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9a_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_1fcdba9a2e7f9d1311a06138e820e9a_topic_state_sequence_idx" ON public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" USING btree (topic, state, sequence);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_created_sequence_idx" ON public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" USING btree (created, sequence);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_input_json_idx" ON public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" USING gin (input_json);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_reference_idx" ON public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" USING btree (reference);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_state_owner_idx" ON public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" USING btree (state, owner);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a061_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_1fcdba9a2e7f9d1311a061_topic_identifier_state_sequen_idx" ON public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_20732de50941fcf4857ca9_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_20732de50941fcf4857ca9_topic_identifier_state_sequen_idx" ON public."TASK_P_20732de50941fcf4857ca9b81c3429e2" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_20732de50941fcf4857ca9b81c3429_topic_state_completed_idx" ON public."TASK_P_20732de50941fcf4857ca9b81c3429e2" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_20732de50941fcf4857ca9b81c3429e2_created_sequence_idx" ON public."TASK_P_20732de50941fcf4857ca9b81c3429e2" USING btree (created, sequence);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_20732de50941fcf4857ca9b81c3429e2_input_json_idx" ON public."TASK_P_20732de50941fcf4857ca9b81c3429e2" USING gin (input_json);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_20732de50941fcf4857ca9b81c3429e2_reference_idx" ON public."TASK_P_20732de50941fcf4857ca9b81c3429e2" USING btree (reference);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_20732de50941fcf4857ca9b81c3429e2_state_owner_idx" ON public."TASK_P_20732de50941fcf4857ca9b81c3429e2" USING btree (state, owner);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_20732de50941fcf4857ca9b81c3429e_topic_state_sequence_idx" ON public."TASK_P_20732de50941fcf4857ca9b81c3429e2" USING btree (topic, state, sequence);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3902ad9b27d03b0853ff7b2f3084624_topic_state_sequence_idx" ON public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" USING btree (topic, state, sequence);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3902ad9b27d03b0853ff7b2f3084624e_created_sequence_idx" ON public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" USING btree (created, sequence);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3902ad9b27d03b0853ff7b2f3084624e_input_json_idx" ON public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" USING gin (input_json);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3902ad9b27d03b0853ff7b2f3084624e_reference_idx" ON public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" USING btree (reference);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3902ad9b27d03b0853ff7b2f3084624e_state_owner_idx" ON public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" USING btree (state, owner);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f308462_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3902ad9b27d03b0853ff7b2f308462_topic_state_completed_idx" ON public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3902ad9b27d03b0853ff7b_topic_identifier_state_sequen_idx" ON public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_created_sequence_idx" ON public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" USING btree (created, sequence);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_input_json_idx" ON public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" USING gin (input_json);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_reference_idx" ON public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" USING btree (reference);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_state_owner_idx" ON public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" USING btree (state, owner);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf15_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3a2dfdd6a7cd22ee7abab48142daf15_topic_state_sequence_idx" ON public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" USING btree (topic, state, sequence);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf1_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3a2dfdd6a7cd22ee7abab48142daf1_topic_state_completed_idx" ON public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab4_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_3a2dfdd6a7cd22ee7abab4_topic_identifier_state_sequen_idx" ON public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fd_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_40bdb70132e8c1706d4825982010fd_topic_state_completed_idx" ON public."TASK_P_40bdb70132e8c1706d4825982010fdef" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fde_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_40bdb70132e8c1706d4825982010fde_topic_state_sequence_idx" ON public."TASK_P_40bdb70132e8c1706d4825982010fdef" USING btree (topic, state, sequence);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_40bdb70132e8c1706d4825982010fdef_created_sequence_idx" ON public."TASK_P_40bdb70132e8c1706d4825982010fdef" USING btree (created, sequence);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_40bdb70132e8c1706d4825982010fdef_input_json_idx" ON public."TASK_P_40bdb70132e8c1706d4825982010fdef" USING gin (input_json);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_40bdb70132e8c1706d4825982010fdef_reference_idx" ON public."TASK_P_40bdb70132e8c1706d4825982010fdef" USING btree (reference);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_40bdb70132e8c1706d4825982010fdef_state_owner_idx" ON public."TASK_P_40bdb70132e8c1706d4825982010fdef" USING btree (state, owner);


--
-- Name: TASK_P_40bdb70132e8c1706d4825_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_40bdb70132e8c1706d4825_topic_identifier_state_sequen_idx" ON public."TASK_P_40bdb70132e8c1706d4825982010fdef" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_45f7378c55930a4fa766419581bbdb31_created_sequence_idx" ON public."TASK_P_45f7378c55930a4fa766419581bbdb31" USING btree (created, sequence);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_45f7378c55930a4fa766419581bbdb31_input_json_idx" ON public."TASK_P_45f7378c55930a4fa766419581bbdb31" USING gin (input_json);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_45f7378c55930a4fa766419581bbdb31_reference_idx" ON public."TASK_P_45f7378c55930a4fa766419581bbdb31" USING btree (reference);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_45f7378c55930a4fa766419581bbdb31_state_owner_idx" ON public."TASK_P_45f7378c55930a4fa766419581bbdb31" USING btree (state, owner);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb3_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_45f7378c55930a4fa766419581bbdb3_topic_state_sequence_idx" ON public."TASK_P_45f7378c55930a4fa766419581bbdb31" USING btree (topic, state, sequence);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_45f7378c55930a4fa766419581bbdb_topic_state_completed_idx" ON public."TASK_P_45f7378c55930a4fa766419581bbdb31" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_45f7378c55930a4fa76641_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_45f7378c55930a4fa76641_topic_identifier_state_sequen_idx" ON public."TASK_P_45f7378c55930a4fa766419581bbdb31" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4ae_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_488fcfd5aca5830dce22e73168a4ae_topic_state_completed_idx" ON public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aee_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_488fcfd5aca5830dce22e73168a4aee_topic_state_sequence_idx" ON public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" USING btree (topic, state, sequence);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_488fcfd5aca5830dce22e73168a4aeeb_created_sequence_idx" ON public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" USING btree (created, sequence);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_488fcfd5aca5830dce22e73168a4aeeb_input_json_idx" ON public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" USING gin (input_json);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_488fcfd5aca5830dce22e73168a4aeeb_reference_idx" ON public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" USING btree (reference);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_488fcfd5aca5830dce22e73168a4aeeb_state_owner_idx" ON public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" USING btree (state, owner);


--
-- Name: TASK_P_488fcfd5aca5830dce22e7_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_488fcfd5aca5830dce22e7_topic_identifier_state_sequen_idx" ON public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_5bf5fc53e7190396e56324_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_5bf5fc53e7190396e56324_topic_identifier_state_sequen_idx" ON public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_5bf5fc53e7190396e56324d9b61afb0_topic_state_sequence_idx" ON public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" USING btree (topic, state, sequence);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_5bf5fc53e7190396e56324d9b61afb0e_created_sequence_idx" ON public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" USING btree (created, sequence);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_5bf5fc53e7190396e56324d9b61afb0e_input_json_idx" ON public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" USING gin (input_json);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_5bf5fc53e7190396e56324d9b61afb0e_reference_idx" ON public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" USING btree (reference);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_5bf5fc53e7190396e56324d9b61afb0e_state_owner_idx" ON public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" USING btree (state, owner);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_5bf5fc53e7190396e56324d9b61afb_topic_state_completed_idx" ON public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_63928464dc2964de4964545de911bb_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_63928464dc2964de4964545de911bb_topic_state_completed_idx" ON public."TASK_P_63928464dc2964de4964545de911bbfa" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_63928464dc2964de4964545de911bbf_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_63928464dc2964de4964545de911bbf_topic_state_sequence_idx" ON public."TASK_P_63928464dc2964de4964545de911bbfa" USING btree (topic, state, sequence);


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_63928464dc2964de4964545de911bbfa_created_sequence_idx" ON public."TASK_P_63928464dc2964de4964545de911bbfa" USING btree (created, sequence);


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_63928464dc2964de4964545de911bbfa_input_json_idx" ON public."TASK_P_63928464dc2964de4964545de911bbfa" USING gin (input_json);


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_63928464dc2964de4964545de911bbfa_reference_idx" ON public."TASK_P_63928464dc2964de4964545de911bbfa" USING btree (reference);


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_63928464dc2964de4964545de911bbfa_state_owner_idx" ON public."TASK_P_63928464dc2964de4964545de911bbfa" USING btree (state, owner);


--
-- Name: TASK_P_63928464dc2964de496454_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_63928464dc2964de496454_topic_identifier_state_sequen_idx" ON public."TASK_P_63928464dc2964de4964545de911bbfa" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_created_sequence_idx" ON public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" USING btree (created, sequence);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_input_json_idx" ON public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" USING gin (input_json);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_reference_idx" ON public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" USING btree (reference);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_state_owner_idx" ON public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" USING btree (state, owner);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f5_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_68f9afb6bcb580e5ae379b29feaf8f5_topic_state_sequence_idx" ON public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" USING btree (topic, state, sequence);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_68f9afb6bcb580e5ae379b29feaf8f_topic_state_completed_idx" ON public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_68f9afb6bcb580e5ae379b_topic_identifier_state_sequen_idx" ON public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_79d91bae8fe6a0628d436e816a8615_topic_state_completed_idx" ON public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_79d91bae8fe6a0628d436e816a8615d4_created_sequence_idx" ON public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" USING btree (created, sequence);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_79d91bae8fe6a0628d436e816a8615d4_input_json_idx" ON public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" USING gin (input_json);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_79d91bae8fe6a0628d436e816a8615d4_reference_idx" ON public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" USING btree (reference);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_79d91bae8fe6a0628d436e816a8615d4_state_owner_idx" ON public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" USING btree (state, owner);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_79d91bae8fe6a0628d436e816a8615d_topic_state_sequence_idx" ON public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" USING btree (topic, state, sequence);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_79d91bae8fe6a0628d436e_topic_identifier_state_sequen_idx" ON public."TASK_P_79d91bae8fe6a0628d436e816a8615d4" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_acbd18db4cc2f85cedef654fccc4a4_topic_state_completed_idx" ON public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_created_sequence_idx" ON public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" USING btree (created, sequence);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_input_json_idx" ON public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" USING gin (input_json);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_reference_idx" ON public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" USING btree (reference);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_state_owner_idx" ON public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" USING btree (state, owner);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_acbd18db4cc2f85cedef654fccc4a4d_topic_state_sequence_idx" ON public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" USING btree (topic, state, sequence);


--
-- Name: TASK_P_acbd18db4cc2f85cedef65_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_acbd18db4cc2f85cedef65_topic_identifier_state_sequen_idx" ON public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_cfb65eebf1137eeff4427d58ff8ab7_topic_state_completed_idx" ON public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_created_sequence_idx" ON public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" USING btree (created, sequence);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_input_json_idx" ON public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" USING gin (input_json);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_reference_idx" ON public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" USING btree (reference);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_state_owner_idx" ON public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" USING btree (state, owner);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_cfb65eebf1137eeff4427d58ff8ab7e_topic_state_sequence_idx" ON public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" USING btree (topic, state, sequence);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_cfb65eebf1137eeff4427d_topic_identifier_state_sequen_idx" ON public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_eb7439b5e584e262a89b85853948fe65_created_sequence_idx" ON public."TASK_P_eb7439b5e584e262a89b85853948fe65" USING btree (created, sequence);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_eb7439b5e584e262a89b85853948fe65_input_json_idx" ON public."TASK_P_eb7439b5e584e262a89b85853948fe65" USING gin (input_json);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_eb7439b5e584e262a89b85853948fe65_reference_idx" ON public."TASK_P_eb7439b5e584e262a89b85853948fe65" USING btree (reference);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_eb7439b5e584e262a89b85853948fe65_state_owner_idx" ON public."TASK_P_eb7439b5e584e262a89b85853948fe65" USING btree (state, owner);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe6_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_eb7439b5e584e262a89b85853948fe6_topic_state_sequence_idx" ON public."TASK_P_eb7439b5e584e262a89b85853948fe65" USING btree (topic, state, sequence);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_eb7439b5e584e262a89b85853948fe_topic_state_completed_idx" ON public."TASK_P_eb7439b5e584e262a89b85853948fe65" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_eb7439b5e584e262a89b85_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_eb7439b5e584e262a89b85_topic_identifier_state_sequen_idx" ON public."TASK_P_eb7439b5e584e262a89b85853948fe65" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_ef84f5b2c149fc8d0e46ee7963fa33_topic_state_completed_idx" ON public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33a_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_ef84f5b2c149fc8d0e46ee7963fa33a_topic_state_sequence_idx" ON public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" USING btree (topic, state, sequence);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_created_sequence_idx" ON public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" USING btree (created, sequence);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_input_json_idx" ON public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" USING gin (input_json);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_reference_idx" ON public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" USING btree (reference);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_state_owner_idx" ON public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" USING btree (state, owner);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_ef84f5b2c149fc8d0e46ee_topic_identifier_state_sequen_idx" ON public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_f61892b9a6831782463551_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_f61892b9a6831782463551_topic_identifier_state_sequen_idx" ON public."TASK_P_f61892b9a6831782463551e84ea4fb1f" USING btree (topic, identifier, state, sequence);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_f61892b9a6831782463551e84ea4fb1_topic_state_sequence_idx" ON public."TASK_P_f61892b9a6831782463551e84ea4fb1f" USING btree (topic, state, sequence);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_f61892b9a6831782463551e84ea4fb1f_created_sequence_idx" ON public."TASK_P_f61892b9a6831782463551e84ea4fb1f" USING btree (created, sequence);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_f61892b9a6831782463551e84ea4fb1f_input_json_idx" ON public."TASK_P_f61892b9a6831782463551e84ea4fb1f" USING gin (input_json);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_f61892b9a6831782463551e84ea4fb1f_reference_idx" ON public."TASK_P_f61892b9a6831782463551e84ea4fb1f" USING btree (reference);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_f61892b9a6831782463551e84ea4fb1f_state_owner_idx" ON public."TASK_P_f61892b9a6831782463551e84ea4fb1f" USING btree (state, owner);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_f61892b9a6831782463551e84ea4fb_topic_state_completed_idx" ON public."TASK_P_f61892b9a6831782463551e84ea4fb1f" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92_topic_state_completed_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_fea3894a27c696803b0e286e867f92_topic_state_completed_idx" ON public."TASK_P_fea3894a27c696803b0e286e867f92ae" USING btree (topic, state, completed) WHERE ((state >= (3)::numeric) AND (state <= (6)::numeric) AND (completed IS NOT NULL));


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92a_topic_state_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_fea3894a27c696803b0e286e867f92a_topic_state_sequence_idx" ON public."TASK_P_fea3894a27c696803b0e286e867f92ae" USING btree (topic, state, sequence);


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_created_sequence_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_fea3894a27c696803b0e286e867f92ae_created_sequence_idx" ON public."TASK_P_fea3894a27c696803b0e286e867f92ae" USING btree (created, sequence);


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_input_json_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_fea3894a27c696803b0e286e867f92ae_input_json_idx" ON public."TASK_P_fea3894a27c696803b0e286e867f92ae" USING gin (input_json);


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_reference_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_fea3894a27c696803b0e286e867f92ae_reference_idx" ON public."TASK_P_fea3894a27c696803b0e286e867f92ae" USING btree (reference);


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_state_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_fea3894a27c696803b0e286e867f92ae_state_owner_idx" ON public."TASK_P_fea3894a27c696803b0e286e867f92ae" USING btree (state, owner);


--
-- Name: TASK_P_fea3894a27c696803b0e28_topic_identifier_state_sequen_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "TASK_P_fea3894a27c696803b0e28_topic_identifier_state_sequen_idx" ON public."TASK_P_fea3894a27c696803b0e286e867f92ae" USING btree (topic, identifier, state, sequence);


--
-- Name: idx_task_tree_createed; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_task_tree_createed ON public.task_tree USING btree (created_at);


--
-- Name: idx_task_tree_levels; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_task_tree_levels ON public.task_tree USING btree (public.nlevel(levels));


--
-- Name: task_owner_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_owner_idx ON public.task_owner USING btree (heartbeat);


--
-- Name: task_tree_meta_gin_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX task_tree_meta_gin_idx ON public.task_tree USING gin (meta);


--
-- Name: TASK_P_066b15f7e8551737cd1435_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba85_created_sequence_idx";


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba85_input_json_idx";


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba85_pkey";


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba85_reference_idx";


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba85_state_owner_idx";


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba8_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba8_topic_state_sequence_idx";


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_066b15f7e8551737cd1435c5db56ba_topic_state_completed_idx";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4_topic_state_completed_idx";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_created_sequence_idx";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_input_json_idx";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_pkey";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_reference_idx";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7_state_owner_idx";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e_topic_state_sequence_idx";


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_0f89e2e28584f2ca1d7b98_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9_topic_state_completed_idx";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9a_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9a_topic_state_sequence_idx";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_created_sequence_idx";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_input_json_idx";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_pkey";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_reference_idx";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad_state_owner_idx";


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a061_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_1fcdba9a2e7f9d1311a061_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_20732de50941fcf4857ca9_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429_topic_state_completed_idx";


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429e2_created_sequence_idx";


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429e2_input_json_idx";


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429e2_pkey";


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429e2_reference_idx";


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429e2_state_owner_idx";


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_20732de50941fcf4857ca9b81c3429e_topic_state_sequence_idx";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f3084624_topic_state_sequence_idx";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e_created_sequence_idx";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e_input_json_idx";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e_pkey";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e_reference_idx";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e_state_owner_idx";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f308462_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b2f308462_topic_state_completed_idx";


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_3902ad9b27d03b0853ff7b_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_created_sequence_idx";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_input_json_idx";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_pkey";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_reference_idx";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157_state_owner_idx";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf15_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf15_topic_state_sequence_idx";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf1_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf1_topic_state_completed_idx";


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab4_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_3a2dfdd6a7cd22ee7abab4_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fd_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fd_topic_state_completed_idx";


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fde_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fde_topic_state_sequence_idx";


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fdef_created_sequence_idx";


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fdef_input_json_idx";


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fdef_pkey";


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fdef_reference_idx";


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825982010fdef_state_owner_idx";


--
-- Name: TASK_P_40bdb70132e8c1706d4825_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_40bdb70132e8c1706d4825_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb31_created_sequence_idx";


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb31_input_json_idx";


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb31_pkey";


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb31_reference_idx";


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb31_state_owner_idx";


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb3_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb3_topic_state_sequence_idx";


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_45f7378c55930a4fa766419581bbdb_topic_state_completed_idx";


--
-- Name: TASK_P_45f7378c55930a4fa76641_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_45f7378c55930a4fa76641_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4ae_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4ae_topic_state_completed_idx";


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aee_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4aee_topic_state_sequence_idx";


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb_created_sequence_idx";


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb_input_json_idx";


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb_pkey";


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb_reference_idx";


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb_state_owner_idx";


--
-- Name: TASK_P_488fcfd5aca5830dce22e7_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_488fcfd5aca5830dce22e7_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_5bf5fc53e7190396e56324_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb0_topic_state_sequence_idx";


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e_created_sequence_idx";


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e_input_json_idx";


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e_pkey";


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e_reference_idx";


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e_state_owner_idx";


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_5bf5fc53e7190396e56324d9b61afb_topic_state_completed_idx";


--
-- Name: TASK_P_63928464dc2964de4964545de911bb_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bb_topic_state_completed_idx";


--
-- Name: TASK_P_63928464dc2964de4964545de911bbf_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bbf_topic_state_sequence_idx";


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bbfa_created_sequence_idx";


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bbfa_input_json_idx";


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bbfa_pkey";


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bbfa_reference_idx";


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_63928464dc2964de4964545de911bbfa_state_owner_idx";


--
-- Name: TASK_P_63928464dc2964de496454_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_63928464dc2964de496454_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_created_sequence_idx";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_input_json_idx";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_pkey";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_reference_idx";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54_state_owner_idx";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f5_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f5_topic_state_sequence_idx";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f_topic_state_completed_idx";


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_68f9afb6bcb580e5ae379b_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615_topic_state_completed_idx";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615d4_created_sequence_idx";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615d4_input_json_idx";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615d4_pkey";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615d4_reference_idx";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615d4_state_owner_idx";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e816a8615d_topic_state_sequence_idx";


--
-- Name: TASK_P_79d91bae8fe6a0628d436e_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_79d91bae8fe6a0628d436e_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4_topic_state_completed_idx";


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_created_sequence_idx";


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_input_json_idx";


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_pkey";


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_reference_idx";


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8_state_owner_idx";


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d_topic_state_sequence_idx";


--
-- Name: TASK_P_acbd18db4cc2f85cedef65_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_acbd18db4cc2f85cedef65_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7_topic_state_completed_idx";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_created_sequence_idx";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_input_json_idx";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_pkey";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_reference_idx";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8_state_owner_idx";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e_topic_state_sequence_idx";


--
-- Name: TASK_P_cfb65eebf1137eeff4427d_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_cfb65eebf1137eeff4427d_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe65_created_sequence_idx";


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe65_input_json_idx";


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe65_pkey";


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe65_reference_idx";


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe65_state_owner_idx";


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe6_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe6_topic_state_sequence_idx";


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85853948fe_topic_state_completed_idx";


--
-- Name: TASK_P_eb7439b5e584e262a89b85_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_eb7439b5e584e262a89b85_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33_topic_state_completed_idx";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33a_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33a_topic_state_sequence_idx";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_created_sequence_idx";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_input_json_idx";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_pkey";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_reference_idx";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae_state_owner_idx";


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_ef84f5b2c149fc8d0e46ee_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_f61892b9a6831782463551_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_f61892b9a6831782463551_topic_identifier_state_sequen_idx";


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb1_topic_state_sequence_idx";


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb1f_created_sequence_idx";


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb1f_input_json_idx";


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb1f_pkey";


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb1f_reference_idx";


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb1f_state_owner_idx";


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_f61892b9a6831782463551e84ea4fb_topic_state_completed_idx";


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92_topic_state_completed_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_completion_idx ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92_topic_state_completed_idx";


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92a_topic_state_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_topic_idx ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92a_topic_state_sequence_idx";


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_created_sequence_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_creation ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92ae_created_sequence_idx";


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_input_json_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.idx_task_input_json ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92ae_input_json_idx";


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_pkey; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_pk ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92ae_pkey";


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_reference_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_reference_idx ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92ae_reference_idx";


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae_state_owner_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_owner_idx ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e286e867f92ae_state_owner_idx";


--
-- Name: TASK_P_fea3894a27c696803b0e28_topic_identifier_state_sequen_idx; Type: INDEX ATTACH; Schema: public; Owner: postgres
--

ALTER INDEX public.task_by_id_idx ATTACH PARTITION public."TASK_P_fea3894a27c696803b0e28_topic_identifier_state_sequen_idx";


--
-- Name: task_activation task_activation_notify_trg; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER task_activation_notify_trg AFTER INSERT OR UPDATE ON public.task_activation FOR EACH ROW EXECUTE FUNCTION public.task_activation_notify_fct();


--
-- Name: task_topic task_notify_insert_trg; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER task_notify_insert_trg AFTER INSERT ON public.task_topic FOR EACH ROW EXECUTE FUNCTION public.task_notify_insert_fct();


--
-- Name: task_topic task_topic_drop_trg; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER task_topic_drop_trg AFTER DELETE ON public.task_topic FOR EACH ROW EXECUTE FUNCTION public.task_topic_drop_fct();


--
-- Name: task_topic task_topic_trg; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER task_topic_trg AFTER INSERT ON public.task_topic FOR EACH ROW EXECUTE FUNCTION public.task_topic_fct();


--
-- Name: Classifier_Vallia_Field_Mappings FK_Classifier_Vallia_Field_Mappings_Doc_Classifier_Fields_clas~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Classifier_Vallia_Field_Mappings"
    ADD CONSTRAINT "FK_Classifier_Vallia_Field_Mappings_Doc_Classifier_Fields_clas~" FOREIGN KEY (classifier_field_id) REFERENCES public."Doc_Classifier_Fields"(id);


--
-- Name: Classifier_Vallia_Field_Mappings FK_Classifier_Vallia_Field_Mappings_Vallia_Fields_vallia_field~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Classifier_Vallia_Field_Mappings"
    ADD CONSTRAINT "FK_Classifier_Vallia_Field_Mappings_Vallia_Fields_vallia_field~" FOREIGN KEY (vallia_field_id) REFERENCES public."Vallia_Fields"(id);


--
-- Name: Criteria_Options FK_Criteria_Options_Criteria_Types_criteria_type_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Criteria_Options"
    ADD CONSTRAINT "FK_Criteria_Options_Criteria_Types_criteria_type_id" FOREIGN KEY (criteria_type_id) REFERENCES public."Criteria_Types"(id);


--
-- Name: Doc_Classifier_Document_Categories FK_Doc_Classifier_Document_Categories_Doc_Classifiers_doc_clas~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Document_Categories"
    ADD CONSTRAINT "FK_Doc_Classifier_Document_Categories_Doc_Classifiers_doc_clas~" FOREIGN KEY (doc_classifier_id) REFERENCES public."Doc_Classifiers"(id);


--
-- Name: Doc_Classifier_Document_Categories FK_Doc_Classifier_Document_Categories_Vallia_Document_Categori~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Document_Categories"
    ADD CONSTRAINT "FK_Doc_Classifier_Document_Categories_Vallia_Document_Categori~" FOREIGN KEY (vallia_doc_category_id) REFERENCES public."Vallia_Document_Categories"(id);


--
-- Name: Doc_Classifier_Documents FK_Doc_Classifier_Documents_Doc_Classifier_Document_Categories~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Documents"
    ADD CONSTRAINT "FK_Doc_Classifier_Documents_Doc_Classifier_Document_Categories~" FOREIGN KEY (document_category_id) REFERENCES public."Doc_Classifier_Document_Categories"(id);


--
-- Name: Doc_Classifier_Documents FK_Doc_Classifier_Documents_Vallia_Documents_vallia_document_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Documents"
    ADD CONSTRAINT "FK_Doc_Classifier_Documents_Vallia_Documents_vallia_document_id" FOREIGN KEY (vallia_document_id) REFERENCES public."Vallia_Documents"(id);


--
-- Name: Doc_Classifier_Fields FK_Doc_Classifier_Fields_Doc_Classifier_Documents_doc_classifi~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Doc_Classifier_Fields"
    ADD CONSTRAINT "FK_Doc_Classifier_Fields_Doc_Classifier_Documents_doc_classifi~" FOREIGN KEY (doc_classifier_document_id) REFERENCES public."Doc_Classifier_Documents"(id);


--
-- Name: Document_Upload_Failure_Status FK_Document_Upload_Failure_Status_Tenant_tenant_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Upload_Failure_Status"
    ADD CONSTRAINT "FK_Document_Upload_Failure_Status_Tenant_tenant_id" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: Field_SOT_Mappings FK_Field_SOT_Mappings_System_Of_Record_Fields_sor_field_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Field_SOT_Mappings"
    ADD CONSTRAINT "FK_Field_SOT_Mappings_System_Of_Record_Fields_sor_field_id" FOREIGN KEY (sor_field_id) REFERENCES public."System_Of_Record_Fields"(id);


--
-- Name: Field_SOT_Mappings FK_Field_SOT_Mappings_Tenant_Tenant_Id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Field_SOT_Mappings"
    ADD CONSTRAINT "FK_Field_SOT_Mappings_Tenant_Tenant_Id" FOREIGN KEY ("Tenant_Id") REFERENCES public."Tenant"(id);


--
-- Name: Field_SOT_Mappings FK_Field_SOT_Mappings_Vallia_Documents_vallia_document_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Field_SOT_Mappings"
    ADD CONSTRAINT "FK_Field_SOT_Mappings_Vallia_Documents_vallia_document_id" FOREIGN KEY (vallia_document_id) REFERENCES public."Vallia_Documents"(id);


--
-- Name: Field_SOT_Mappings FK_Field_SOT_Mappings_Vallia_Fields_vallia_field_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Field_SOT_Mappings"
    ADD CONSTRAINT "FK_Field_SOT_Mappings_Vallia_Fields_vallia_field_id" FOREIGN KEY (vallia_field_id) REFERENCES public."Vallia_Fields"(id);


--
-- Name: Global_Flow_Flow_Rules_Mappings FK_Global_Flow_Flow_Rules_Mappings_Global_Flow_Rules_flow_rule~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Global_Flow_Flow_Rules_Mappings"
    ADD CONSTRAINT "FK_Global_Flow_Flow_Rules_Mappings_Global_Flow_Rules_flow_rule~" FOREIGN KEY (flow_rule_id) REFERENCES public."Global_Flow_Rules"(id) ON DELETE CASCADE;


--
-- Name: Global_Flow_Flow_Rules_Mappings FK_Global_Flow_Flow_Rules_Mappings_Global_Flows_flow_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Global_Flow_Flow_Rules_Mappings"
    ADD CONSTRAINT "FK_Global_Flow_Flow_Rules_Mappings_Global_Flows_flow_id" FOREIGN KEY (flow_id) REFERENCES public."Global_Flows"(id) ON DELETE CASCADE;


--
-- Name: Global_Flow_Rules FK_Global_Flow_Rules_Vallia_Documents_trigger_resource_documen~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Global_Flow_Rules"
    ADD CONSTRAINT "FK_Global_Flow_Rules_Vallia_Documents_trigger_resource_documen~" FOREIGN KEY (trigger_resource_document_id) REFERENCES public."Vallia_Documents"(id);


--
-- Name: Ocrolus_Books FK_Ocrolus_Books_Tenant_tenant_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Ocrolus_Books"
    ADD CONSTRAINT "FK_Ocrolus_Books_Tenant_tenant_id" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id) ON DELETE CASCADE;


--
-- Name: Ocrolus_Documents FK_Ocrolus_Documents_Ocrolus_Books_book_uuid; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Ocrolus_Documents"
    ADD CONSTRAINT "FK_Ocrolus_Documents_Ocrolus_Books_book_uuid" FOREIGN KEY (book_uuid) REFERENCES public."Ocrolus_Books"(book_uuid) ON DELETE CASCADE;


--
-- Name: System_Of_Record_Fields FK_System_Of_Record_Fields_System_Of_Records_sor_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_Of_Record_Fields"
    ADD CONSTRAINT "FK_System_Of_Record_Fields_System_Of_Records_sor_id" FOREIGN KEY (sor_id) REFERENCES public."System_Of_Records"(id);


--
-- Name: System_Of_Record_Fields FK_System_Of_Record_Fields_Tenant_tenant_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_Of_Record_Fields"
    ADD CONSTRAINT "FK_System_Of_Record_Fields_Tenant_tenant_id" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: System_of_Record_Folders FK_System_of_Record_Folders_Tenant_tenant_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_of_Record_Folders"
    ADD CONSTRAINT "FK_System_of_Record_Folders_Tenant_tenant_id" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: Tenant_Connections FK_Tenant_Connections_Tenant_tenant_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Connections"
    ADD CONSTRAINT "FK_Tenant_Connections_Tenant_tenant_id" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: Tenant FK_Tenant_Doc_Classifiers_doc_classifier_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant"
    ADD CONSTRAINT "FK_Tenant_Doc_Classifiers_doc_classifier_id" FOREIGN KEY (doc_classifier_id) REFERENCES public."Doc_Classifiers"(id);


--
-- Name: Tenant_Flow_Rules_Activities FK_Tenant_Flow_Rules_Activities_Tenant_Flow_Rules_flow_rule_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flow_Rules_Activities"
    ADD CONSTRAINT "FK_Tenant_Flow_Rules_Activities_Tenant_Flow_Rules_flow_rule_id" FOREIGN KEY (flow_rule_id) REFERENCES public."Tenant_Flow_Rules"(id);


--
-- Name: Tenant_Flow_Rules_Activities FK_Tenant_Flow_Rules_Activities_Tenant_Flows_Activities_tenant~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flow_Rules_Activities"
    ADD CONSTRAINT "FK_Tenant_Flow_Rules_Activities_Tenant_Flows_Activities_tenant~" FOREIGN KEY (tenant_flow_activity_id) REFERENCES public."Tenant_Flows_Activities"(id);


--
-- Name: Tenant_Flow_Rules FK_Tenant_Flow_Rules_Global_Flow_Rules_global_flow_rule_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flow_Rules"
    ADD CONSTRAINT "FK_Tenant_Flow_Rules_Global_Flow_Rules_global_flow_rule_id" FOREIGN KEY (global_flow_rule_id) REFERENCES public."Global_Flow_Rules"(id);


--
-- Name: Tenant_Flow_Rules FK_Tenant_Flow_Rules_Tenant_Flows_tenant_flow_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flow_Rules"
    ADD CONSTRAINT "FK_Tenant_Flow_Rules_Tenant_Flows_tenant_flow_id" FOREIGN KEY (tenant_flow_id) REFERENCES public."Tenant_Flows"(id);


--
-- Name: Tenant_Flow_Rules FK_Tenant_Flow_Rules_Vallia_Documents_trigger_resource_documen~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flow_Rules"
    ADD CONSTRAINT "FK_Tenant_Flow_Rules_Vallia_Documents_trigger_resource_documen~" FOREIGN KEY (trigger_resource_document_id) REFERENCES public."Vallia_Documents"(id);


--
-- Name: Tenant_Flows_Activities FK_Tenant_Flows_Activities_Tenant_Flows_tenant_flow_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flows_Activities"
    ADD CONSTRAINT "FK_Tenant_Flows_Activities_Tenant_Flows_tenant_flow_id" FOREIGN KEY (tenant_flow_id) REFERENCES public."Tenant_Flows"(id);


--
-- Name: Tenant_Flows FK_Tenant_Flows_Global_Flows_global_flow_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flows"
    ADD CONSTRAINT "FK_Tenant_Flows_Global_Flows_global_flow_id" FOREIGN KEY (global_flow_id) REFERENCES public."Global_Flows"(id);


--
-- Name: Tenant_Flows FK_Tenant_Flows_Tenant_tenant_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant_Flows"
    ADD CONSTRAINT "FK_Tenant_Flows_Tenant_tenant_id" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: Tenant FK_Tenant_System_Of_Records_sor_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Tenant"
    ADD CONSTRAINT "FK_Tenant_System_Of_Records_sor_id" FOREIGN KEY (sor_id) REFERENCES public."System_Of_Records"(id);


--
-- Name: Vallia_Documents FK_Vallia_Documents_Vallia_Document_Categories_document_catego~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Documents"
    ADD CONSTRAINT "FK_Vallia_Documents_Vallia_Document_Categories_document_catego~" FOREIGN KEY (document_category_id) REFERENCES public."Vallia_Document_Categories"(id);


--
-- Name: Vallia_Sor_Document_Mappings FK_Vallia_Sor_Document_Mappings_System_of_Record_Folders_sor_f~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Sor_Document_Mappings"
    ADD CONSTRAINT "FK_Vallia_Sor_Document_Mappings_System_of_Record_Folders_sor_f~" FOREIGN KEY (sor_folder_id) REFERENCES public."System_of_Record_Folders"(id);


--
-- Name: Vallia_Sor_Document_Mappings FK_Vallia_Sor_Document_Mappings_Vallia_Documents_vallia_docume~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Sor_Document_Mappings"
    ADD CONSTRAINT "FK_Vallia_Sor_Document_Mappings_Vallia_Documents_vallia_docume~" FOREIGN KEY (vallia_document_id) REFERENCES public."Vallia_Documents"(id);


--
-- Name: Vallia_Sor_Field_Mappings FK_Vallia_Sor_Field_Mappings_System_Of_Record_Fields_sor_field~; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Sor_Field_Mappings"
    ADD CONSTRAINT "FK_Vallia_Sor_Field_Mappings_System_Of_Record_Fields_sor_field~" FOREIGN KEY (sor_field_id) REFERENCES public."System_Of_Record_Fields"(id);


--
-- Name: Vallia_Sor_Field_Mappings FK_Vallia_Sor_Field_Mappings_Vallia_Fields_vallia_field_id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Vallia_Sor_Field_Mappings"
    ADD CONSTRAINT "FK_Vallia_Sor_Field_Mappings_Vallia_Fields_vallia_field_id" FOREIGN KEY (vallia_field_id) REFERENCES public."Vallia_Fields"(id);


--
-- Name: hooks FK_tenant_settings; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.hooks
    ADD CONSTRAINT "FK_tenant_settings" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85 TASK_OWNER_REF_066b15f7e8551737cd1435c5db56ba85; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_066b15f7e8551737cd1435c5db56ba85"
    ADD CONSTRAINT "TASK_OWNER_REF_066b15f7e8551737cd1435c5db56ba85" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7 TASK_OWNER_REF_0f89e2e28584f2ca1d7b98823544f4e7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7"
    ADD CONSTRAINT "TASK_OWNER_REF_0f89e2e28584f2ca1d7b98823544f4e7" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad TASK_OWNER_REF_1fcdba9a2e7f9d1311a06138e820e9ad; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad"
    ADD CONSTRAINT "TASK_OWNER_REF_1fcdba9a2e7f9d1311a06138e820e9ad" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2 TASK_OWNER_REF_20732de50941fcf4857ca9b81c3429e2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_20732de50941fcf4857ca9b81c3429e2"
    ADD CONSTRAINT "TASK_OWNER_REF_20732de50941fcf4857ca9b81c3429e2" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e TASK_OWNER_REF_3902ad9b27d03b0853ff7b2f3084624e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e"
    ADD CONSTRAINT "TASK_OWNER_REF_3902ad9b27d03b0853ff7b2f3084624e" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157 TASK_OWNER_REF_3a2dfdd6a7cd22ee7abab48142daf157; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157"
    ADD CONSTRAINT "TASK_OWNER_REF_3a2dfdd6a7cd22ee7abab48142daf157" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef TASK_OWNER_REF_40bdb70132e8c1706d4825982010fdef; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_40bdb70132e8c1706d4825982010fdef"
    ADD CONSTRAINT "TASK_OWNER_REF_40bdb70132e8c1706d4825982010fdef" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31 TASK_OWNER_REF_45f7378c55930a4fa766419581bbdb31; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_45f7378c55930a4fa766419581bbdb31"
    ADD CONSTRAINT "TASK_OWNER_REF_45f7378c55930a4fa766419581bbdb31" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb TASK_OWNER_REF_488fcfd5aca5830dce22e73168a4aeeb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb"
    ADD CONSTRAINT "TASK_OWNER_REF_488fcfd5aca5830dce22e73168a4aeeb" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e TASK_OWNER_REF_5bf5fc53e7190396e56324d9b61afb0e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e"
    ADD CONSTRAINT "TASK_OWNER_REF_5bf5fc53e7190396e56324d9b61afb0e" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa TASK_OWNER_REF_63928464dc2964de4964545de911bbfa; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_63928464dc2964de4964545de911bbfa"
    ADD CONSTRAINT "TASK_OWNER_REF_63928464dc2964de4964545de911bbfa" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54 TASK_OWNER_REF_68f9afb6bcb580e5ae379b29feaf8f54; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54"
    ADD CONSTRAINT "TASK_OWNER_REF_68f9afb6bcb580e5ae379b29feaf8f54" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4 TASK_OWNER_REF_79d91bae8fe6a0628d436e816a8615d4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_79d91bae8fe6a0628d436e816a8615d4"
    ADD CONSTRAINT "TASK_OWNER_REF_79d91bae8fe6a0628d436e816a8615d4" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8 TASK_OWNER_REF_acbd18db4cc2f85cedef654fccc4a4d8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8"
    ADD CONSTRAINT "TASK_OWNER_REF_acbd18db4cc2f85cedef654fccc4a4d8" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8 TASK_OWNER_REF_cfb65eebf1137eeff4427d58ff8ab7e8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8"
    ADD CONSTRAINT "TASK_OWNER_REF_cfb65eebf1137eeff4427d58ff8ab7e8" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65 TASK_OWNER_REF_eb7439b5e584e262a89b85853948fe65; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_eb7439b5e584e262a89b85853948fe65"
    ADD CONSTRAINT "TASK_OWNER_REF_eb7439b5e584e262a89b85853948fe65" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae TASK_OWNER_REF_ef84f5b2c149fc8d0e46ee7963fa33ae; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae"
    ADD CONSTRAINT "TASK_OWNER_REF_ef84f5b2c149fc8d0e46ee7963fa33ae" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f TASK_OWNER_REF_f61892b9a6831782463551e84ea4fb1f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_f61892b9a6831782463551e84ea4fb1f"
    ADD CONSTRAINT "TASK_OWNER_REF_f61892b9a6831782463551e84ea4fb1f" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae TASK_OWNER_REF_fea3894a27c696803b0e286e867f92ae; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_fea3894a27c696803b0e286e867f92ae"
    ADD CONSTRAINT "TASK_OWNER_REF_fea3894a27c696803b0e286e867f92ae" FOREIGN KEY (owner) REFERENCES public.task_owner(owner);


--
-- Name: TASK_P_066b15f7e8551737cd1435c5db56ba85 TASK_TOPIC_REF_066b15f7e8551737cd1435c5db56ba85; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_066b15f7e8551737cd1435c5db56ba85"
    ADD CONSTRAINT "TASK_TOPIC_REF_066b15f7e8551737cd1435c5db56ba85" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_0f89e2e28584f2ca1d7b98823544f4e7 TASK_TOPIC_REF_0f89e2e28584f2ca1d7b98823544f4e7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_0f89e2e28584f2ca1d7b98823544f4e7"
    ADD CONSTRAINT "TASK_TOPIC_REF_0f89e2e28584f2ca1d7b98823544f4e7" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad TASK_TOPIC_REF_1fcdba9a2e7f9d1311a06138e820e9ad; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_1fcdba9a2e7f9d1311a06138e820e9ad"
    ADD CONSTRAINT "TASK_TOPIC_REF_1fcdba9a2e7f9d1311a06138e820e9ad" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_20732de50941fcf4857ca9b81c3429e2 TASK_TOPIC_REF_20732de50941fcf4857ca9b81c3429e2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_20732de50941fcf4857ca9b81c3429e2"
    ADD CONSTRAINT "TASK_TOPIC_REF_20732de50941fcf4857ca9b81c3429e2" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_3902ad9b27d03b0853ff7b2f3084624e TASK_TOPIC_REF_3902ad9b27d03b0853ff7b2f3084624e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_3902ad9b27d03b0853ff7b2f3084624e"
    ADD CONSTRAINT "TASK_TOPIC_REF_3902ad9b27d03b0853ff7b2f3084624e" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_3a2dfdd6a7cd22ee7abab48142daf157 TASK_TOPIC_REF_3a2dfdd6a7cd22ee7abab48142daf157; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_3a2dfdd6a7cd22ee7abab48142daf157"
    ADD CONSTRAINT "TASK_TOPIC_REF_3a2dfdd6a7cd22ee7abab48142daf157" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_40bdb70132e8c1706d4825982010fdef TASK_TOPIC_REF_40bdb70132e8c1706d4825982010fdef; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_40bdb70132e8c1706d4825982010fdef"
    ADD CONSTRAINT "TASK_TOPIC_REF_40bdb70132e8c1706d4825982010fdef" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_45f7378c55930a4fa766419581bbdb31 TASK_TOPIC_REF_45f7378c55930a4fa766419581bbdb31; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_45f7378c55930a4fa766419581bbdb31"
    ADD CONSTRAINT "TASK_TOPIC_REF_45f7378c55930a4fa766419581bbdb31" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_488fcfd5aca5830dce22e73168a4aeeb TASK_TOPIC_REF_488fcfd5aca5830dce22e73168a4aeeb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_488fcfd5aca5830dce22e73168a4aeeb"
    ADD CONSTRAINT "TASK_TOPIC_REF_488fcfd5aca5830dce22e73168a4aeeb" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_5bf5fc53e7190396e56324d9b61afb0e TASK_TOPIC_REF_5bf5fc53e7190396e56324d9b61afb0e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_5bf5fc53e7190396e56324d9b61afb0e"
    ADD CONSTRAINT "TASK_TOPIC_REF_5bf5fc53e7190396e56324d9b61afb0e" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_63928464dc2964de4964545de911bbfa TASK_TOPIC_REF_63928464dc2964de4964545de911bbfa; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_63928464dc2964de4964545de911bbfa"
    ADD CONSTRAINT "TASK_TOPIC_REF_63928464dc2964de4964545de911bbfa" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_68f9afb6bcb580e5ae379b29feaf8f54 TASK_TOPIC_REF_68f9afb6bcb580e5ae379b29feaf8f54; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_68f9afb6bcb580e5ae379b29feaf8f54"
    ADD CONSTRAINT "TASK_TOPIC_REF_68f9afb6bcb580e5ae379b29feaf8f54" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_79d91bae8fe6a0628d436e816a8615d4 TASK_TOPIC_REF_79d91bae8fe6a0628d436e816a8615d4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_79d91bae8fe6a0628d436e816a8615d4"
    ADD CONSTRAINT "TASK_TOPIC_REF_79d91bae8fe6a0628d436e816a8615d4" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_acbd18db4cc2f85cedef654fccc4a4d8 TASK_TOPIC_REF_acbd18db4cc2f85cedef654fccc4a4d8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_acbd18db4cc2f85cedef654fccc4a4d8"
    ADD CONSTRAINT "TASK_TOPIC_REF_acbd18db4cc2f85cedef654fccc4a4d8" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8 TASK_TOPIC_REF_cfb65eebf1137eeff4427d58ff8ab7e8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_cfb65eebf1137eeff4427d58ff8ab7e8"
    ADD CONSTRAINT "TASK_TOPIC_REF_cfb65eebf1137eeff4427d58ff8ab7e8" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_eb7439b5e584e262a89b85853948fe65 TASK_TOPIC_REF_eb7439b5e584e262a89b85853948fe65; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_eb7439b5e584e262a89b85853948fe65"
    ADD CONSTRAINT "TASK_TOPIC_REF_eb7439b5e584e262a89b85853948fe65" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae TASK_TOPIC_REF_ef84f5b2c149fc8d0e46ee7963fa33ae; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_ef84f5b2c149fc8d0e46ee7963fa33ae"
    ADD CONSTRAINT "TASK_TOPIC_REF_ef84f5b2c149fc8d0e46ee7963fa33ae" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_f61892b9a6831782463551e84ea4fb1f TASK_TOPIC_REF_f61892b9a6831782463551e84ea4fb1f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_f61892b9a6831782463551e84ea4fb1f"
    ADD CONSTRAINT "TASK_TOPIC_REF_f61892b9a6831782463551e84ea4fb1f" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: TASK_P_fea3894a27c696803b0e286e867f92ae TASK_TOPIC_REF_fea3894a27c696803b0e286e867f92ae; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."TASK_P_fea3894a27c696803b0e286e867f92ae"
    ADD CONSTRAINT "TASK_TOPIC_REF_fea3894a27c696803b0e286e867f92ae" FOREIGN KEY (topic) REFERENCES public.task_topic(topic);


--
-- Name: change_ledger fk162467s6xrogqlsg49osaf54r; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.change_ledger
    ADD CONSTRAINT fk162467s6xrogqlsg49osaf54r FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: tenant_settings fk21wedcc05b1hcym445fio4g2l; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_settings
    ADD CONSTRAINT fk21wedcc05b1hcym445fio4g2l FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: econsent_tracker fk6friltxfpdus47ahiqnadvnpn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.econsent_tracker
    ADD CONSTRAINT fk6friltxfpdus47ahiqnadvnpn FOREIGN KEY (change_ledger_id) REFERENCES public.change_ledger(id);


--
-- Name: rule_audit fk6hr1o38t2y76g5fm4dtxj8qma; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_audit
    ADD CONSTRAINT fk6hr1o38t2y76g5fm4dtxj8qma FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: Document_Processing_Results fk7ibwxmi4xu8988yxis9u0swhc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Processing_Results"
    ADD CONSTRAINT fk7ibwxmi4xu8988yxis9u0swhc FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: application_users fk93x1uwn8njff5dg4avdhk7ffy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.application_users
    ADD CONSTRAINT fk93x1uwn8njff5dg4avdhk7ffy FOREIGN KEY (client_id) REFERENCES public.client(client_id);


--
-- Name: client_plan fk9ggjxmqstptlj1cjoylvn5wm1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.client_plan
    ADD CONSTRAINT fk9ggjxmqstptlj1cjoylvn5wm1 FOREIGN KEY (plan_id) REFERENCES public.plan(plan_id);


--
-- Name: document_metadata fk9wc6a7r06uq64wsgd09mhfo33; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_metadata
    ADD CONSTRAINT fk9wc6a7r06uq64wsgd09mhfo33 FOREIGN KEY (change_ledger_id) REFERENCES public.change_ledger(id);


--
-- Name: Document_Extraction fk_customer; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Extraction"
    ADD CONSTRAINT fk_customer FOREIGN KEY (doc_process_id) REFERENCES public."Document_Processing_Results"(id);


--
-- Name: classified_documents_results fk_document_processing_result; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.classified_documents_results
    ADD CONSTRAINT fk_document_processing_result FOREIGN KEY (document_processing_result_id) REFERENCES public."Document_Processing_Results"(id);


--
-- Name: investors fk_investors_Tenant; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.investors
    ADD CONSTRAINT "fk_investors_Tenant" FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: rule_result fk_rule_batch; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_result
    ADD CONSTRAINT fk_rule_batch FOREIGN KEY (rule_batch_id) REFERENCES public.rule_batch(id);


--
-- Name: Document_Extraction fk_source_strategy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Extraction"
    ADD CONSTRAINT fk_source_strategy FOREIGN KEY (source_strategy_id) REFERENCES public."Doc_Classifiers"(id);


--
-- Name: Document_Extraction fk_tenant; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Extraction"
    ADD CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: vallia_assist fk_tenant; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vallia_assist
    ADD CONSTRAINT fk_tenant FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: rule_result fk_tenant_rule; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_result
    ADD CONSTRAINT fk_tenant_rule FOREIGN KEY (tenant_rule_id) REFERENCES public.tenant_rule(id);


--
-- Name: user_preference fk_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preference
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.application_users(application_user_id);


--
-- Name: document_metadata fk_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_metadata
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.application_users(application_user_id);


--
-- Name: Document_Processing_Results fkb2r7w4gx6phknrdvmk27vw969; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."Document_Processing_Results"
    ADD CONSTRAINT fkb2r7w4gx6phknrdvmk27vw969 FOREIGN KEY (metadata_id) REFERENCES public.document_metadata(id);


--
-- Name: feature_role_map_details fkbda6e4yuhpuo88g67gd567e84; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature_role_map_details
    ADD CONSTRAINT fkbda6e4yuhpuo88g67gd567e84 FOREIGN KEY (client_id) REFERENCES public.client(client_id);


--
-- Name: application_users fkbglm4uqsjj56c1id78dmwjigc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.application_users
    ADD CONSTRAINT fkbglm4uqsjj56c1id78dmwjigc FOREIGN KEY (role_id) REFERENCES public.config_roles(role_id);


--
-- Name: feature_role_map_details fkbos139kxn1ej0a73r9kqpyd5j; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature_role_map_details
    ADD CONSTRAINT fkbos139kxn1ej0a73r9kqpyd5j FOREIGN KEY (feature_id) REFERENCES public.feature(feature_id);


--
-- Name: user_config fkewosymjsf6yyww5gkuadjtps1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_config
    ADD CONSTRAINT fkewosymjsf6yyww5gkuadjtps1 FOREIGN KEY (client_id) REFERENCES public.client(client_id);


--
-- Name: econsent_tracker fkfolq8ndre4yk45dyj1rcg5bbj; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.econsent_tracker
    ADD CONSTRAINT fkfolq8ndre4yk45dyj1rcg5bbj FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: rule_audit fkg3i6vrwjtbcrhnhk6ywquthbx; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_audit
    ADD CONSTRAINT fkg3i6vrwjtbcrhnhk6ywquthbx FOREIGN KEY (rule_batch_id) REFERENCES public.rule_batch(id);


--
-- Name: feature_config fkgxjd8brospp7no90880amsk6b; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature_config
    ADD CONSTRAINT fkgxjd8brospp7no90880amsk6b FOREIGN KEY (application_id) REFERENCES public.vallia_application(application_id);


--
-- Name: client_plan fkjm05cguoxy3ys4jp78hdcuexk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.client_plan
    ADD CONSTRAINT fkjm05cguoxy3ys4jp78hdcuexk FOREIGN KEY (client_id) REFERENCES public.client(client_id);


--
-- Name: document_metadata fkk5gnqepdxvmketr80sqtrh2q3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_metadata
    ADD CONSTRAINT fkk5gnqepdxvmketr80sqtrh2q3 FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: document_auto_result fkk5gnqepdxvmketr80sqtrh2q3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.document_auto_result
    ADD CONSTRAINT fkk5gnqepdxvmketr80sqtrh2q3 FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: feature_role_map_details fknix8wy3ep06blici6bbmcdu1j; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature_role_map_details
    ADD CONSTRAINT fknix8wy3ep06blici6bbmcdu1j FOREIGN KEY (role_id) REFERENCES public.config_roles(role_id);


--
-- Name: plan fknj35cimob5qjx3qq44tqcqi2q; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.plan
    ADD CONSTRAINT fknj35cimob5qjx3qq44tqcqi2q FOREIGN KEY (application_id) REFERENCES public.vallia_application(application_id);


--
-- Name: feature_role_map_details fknxbhoy9ym505r4wca7chf8yfe; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature_role_map_details
    ADD CONSTRAINT fknxbhoy9ym505r4wca7chf8yfe FOREIGN KEY (application_id) REFERENCES public.vallia_application(application_id);


--
-- Name: System_of_Record_Folders fko8ggad8bm97mnrnkvo38nukr4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public."System_of_Record_Folders"
    ADD CONSTRAINT fko8ggad8bm97mnrnkvo38nukr4 FOREIGN KEY (vallia_document_id) REFERENCES public."Vallia_Documents"(id);


--
-- Name: user_config fkqfbvciko0uqan8hvgbe3omd5k; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_config
    ADD CONSTRAINT fkqfbvciko0uqan8hvgbe3omd5k FOREIGN KEY (feature_config_id) REFERENCES public.feature_config(feature_config_id);


--
-- Name: feature fkrobken8s03oh2mhxbny1r3tnr; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.feature
    ADD CONSTRAINT fkrobken8s03oh2mhxbny1r3tnr FOREIGN KEY (application_id) REFERENCES public.vallia_application(application_id);


--
-- Name: rule_entity rule_entity_doc_extraction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_entity
    ADD CONSTRAINT rule_entity_doc_extraction_id_fkey FOREIGN KEY (doc_extraction_id) REFERENCES public."Document_Extraction"(id) ON DELETE SET NULL;


--
-- Name: rule_entity rule_entity_execution_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_entity
    ADD CONSTRAINT rule_entity_execution_id_fkey FOREIGN KEY (batch_id) REFERENCES public.rule_batch(id) ON DELETE SET NULL;


--
-- Name: rule_batch rule_run_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.rule_batch
    ADD CONSTRAINT rule_run_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id);


--
-- Name: tenant_rule tenant_rule_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tenant_rule
    ADD CONSTRAINT tenant_rule_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public."Tenant"(id) ON DELETE CASCADE;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--

