    
  insert into rule_entity (entity_type, doc_extraction_id, execution_id, extracted_json, doc_qualifier)
values ('paystub', 428, 3, '{}', '{}');

insert into rule_execution (loantimestamp, loannumber, execution_id) 
values (now(), '5040245829', 4);

                

set client_min_messages to 'debug';

select loan_number   from "Document_Extraction" de where de.id = 428;
select re.execution_id from rule_execution re     where re.loannumber= '5040245829';

truncate rule_execution cascade ;


select distinct (de.document_type) from "Document_Extraction" de

create table temp (rule text);



ALTER TABLE public."Tenant" ADD rule_strategy varchar NULL;
update "Tenant"  te set rule_strategy = 'json_rule_engine' where te.id = 7;  

drop table ruleflow_execution cascade;

CREATE TABLE rule_batch(
    id SERIAL PRIMARY KEY,
    loan_time_stamp TIMESTAMP,
    tenant_id INT REFERENCES "Tenant" (id),
    loan_number VARCHAR
);

CREATE TABLE tenant_rule (
    id SERIAL PRIMARY KEY,
    tenant_id INTEGER REFERENCES "Tenant"(id) ON DELETE CASCADE,
    rule_id text  
);

alter table tenant_rule add constraint TenantRuleUniqueIdRuleId UNIQUE(tenant_id , rule_id);
insert into tenant_rule (tenant_id, rule_id) values (7, 'earliest-paystub-within-45-days@1');

select * from tenant_rule; 

CREATE TABLE rule_result (
    id SERIAL PRIMARY KEY,
    rule_batch_id INTEGER REFERENCES rule_batch(id) ON DELETE cascade,
    tenant_rule_id INTEGER references tenant_rule(id) on delete cascade,
    run_status text,
    rule_result boolean,
    rule_output jsonb
);
-- execution_id, rule_id, result, run_status, rule_result


--CREATE TABLE rule_definition (
--    id SERIAL PRIMARY KEY,
--    name VARCHAR(255)
--);

--CREATE TABLE rule_type (
--    id SERIAL PRIMARY KEY,
--    entity_type VARCHAR(255)
--);

--CREATE TABLE rule_entity_type_mapping (
--    id SERIAL PRIMARY KEY,
--    rule_id INTEGER REFERENCES rule(id) ON DELETE CASCADE,
--    rule_entity_id INTEGER REFERENCES rule_type(id) ON DELETE CASCADE
--);





-- Overall status for an a rule run. triggered whenever a loan is changed

drop table ruleflow_execution cascade

alter table rule_entity add column batch_id int references rule_batch(id);


alter table rule_batch  add  column created_at timestamp;
alter table rule_batch  add  column last_updated_at timestamp;



