insert into task_topic (topic) values ('extract-doc-type'), ('extract-entity'), ('run-rules') on conflict do nothing;

create table topic (topic text)



insert into task (topic, identifier, state, descent, owner, created,input) 
values('extract-doc-type',  'doc_extraction_id', 1, 3, 'foo', now(), 'another_try');

alter table "Document_Extraction" add column doc_qualifier json;
alter table "Document_Extraction" add column file_path text;

CREATE TYPE entity_type AS ENUM (
'paystub.employer',
'urla.employer'
'ausdu.assets'
);

CREATE TABLE rule_entity (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    entity_type entity_type,
    extracted_md text,
    extracted_json json,
    doc_extraction_id integer 
);

create table document_extraction_v2 (
    id BIGSERIAL NOT NULL PRIMARY KEY,    
    extracted_md text,
    extracted_json json,
    doc_extraction_id integer,
    checksum text
)


-- warning : 
alter table "Document_Extraction" drop column tenant_id;


ALTER TABLE rule_entity
    ADD CONSTRAINT "FK_rule_facts_doc_extraction" FOREIGN KEY (doc_extraction_id) REFERENCES "Document_Extraction"(id) ;

alter table rule_entity drop column extracted_md;
alter table rule_entity add column doc_qualifier json;
alter table rule_entity alter column doc_qualifier set not null;
alter table rule_entity alter column extracted_json set not null;
alter table rule_entity add column checksum


ALTER TABLE document_extraction_v2
    ADD CONSTRAINT "FK_rule_facts_doc_extraction" FOREIGN KEY (doc_extraction_id) REFERENCES "Document_Extraction"(id) ;

alter table document_extraction_v2  add constraint "DocExtrV2_UniqChecksum" unique (checksum);

insert into "Document_Extraction" (
    document_type , loan_number ,         
    doc_qualifier , file_path

) values 
  ( 'paystub', '1234',
    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/PAYSTUB-01.pdf'
   );

insert into "Document_Extraction" (    document_type , loan_number ,             doc_qualifier , file_path) 
values 
  ( 'paystub', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/PAYSTUB-01.pdf'
   );

insert into "Document_Extraction" (    document_type , loan_number ,             doc_qualifier , file_path) 
values 
   ( 'paystub', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/PAYSTUB-02.pdf'),
( 'paystub', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/PAYSTUB-04.pdf'),
( 'paystub', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/PAYSTUB-03.pdf'),
( 'mi_insurance_commitment', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/Mortgage Insurance Commitment Certificate.pdf'),
( 'bank-statement', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/bank-02.pdf'),
( 'bank-statement', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/bank-01.pdf'),
( 'note', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/Note.pdf'),

( 'urla', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/1003 - URLA (Signed) bORR1.pdf'),
( 'closing-disclosure', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/CD-partial.pdf'),
( 'auto-underwrite-lp', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/AUS Report - LP.pdf'),
( 'borrower-identification', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/Borrower Identification.pdf'),
( 'drive-score', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/drive_scoring_results -DataVerify_Report.pdf'),
( 'appraisal-report', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/Appraisal.pdf'),
( 'auto-underwrite-du', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/AUS Report - DU.pdf'),
( 'insurance-binder', '',    '{  }', '/home/saro/projects/brimmatech/docflow/ai/pdfs/Insurance Binder.pdf');

insert into rule_entity (entity_type, doc_extraction_id)
values ('paystub', 1);

insert into task (topic, identifier, state, descent, owner, created,input) 
    values('extract-doc-type',  '32', 1, 3, 'foo', now(), null);

insert into task (topic, identifier, state, descent, owner) 
select 'extract-doc-type', id, 1,3,'foo' from "Document_Extraction" where document_type = 'mi_insurance_commitment';

insert into task (topic, identifier, state, descent, owner) 
select 'extract-doc-type', id, 1,3,'foo' from "Document_Extraction" where document_type in 
('urla', 'closing-disclosure', 'auto-underwrite-lp', 'borrower-identification', 'drive-score', 'appraisal-report', 
'auto-underwrite-du', 'insurance-binder');

insert into task (topic, identifier, state, descent, owner) 
select 'extract-entities', id, 1,3,'foo' from "Document_Extraction" where id = 1;

insert into task (topic, identifier, state, descent, owner) 
values( 'run-rules', '1234', 1,3,'rule-engine-caller' );

ALTER TABLE public."Document_Extraction" ALTER COLUMN extracted_json_data TYPE json USING extracted_json_data::text::json;