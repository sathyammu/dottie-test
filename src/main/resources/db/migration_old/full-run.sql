
select tr.rule_id from tenant_rule tr inner join rule_result rr on 
 rr.tenant_rule_id  = tr.id 
 
 
select * from "Document_Extraction" de where de.created_at > now() - interval '40 minutes'




SELECT count(*) FROM pg_stat_activity

WHERE wait_event IS NOT NULL AND backend_type = 'client backend';


select pa.datname, pa.application_name, count(*)                         
 
 from pg_stat_activity pa group by pa.datname, pa.application_name order by count(*) desc

select count(*) from pg_stat_activity where datname = 'vallia_docflow_dev' and application_name = 'PostgreSQL JDBC Driver';

group by application_name, datname having 

SELECT pg_cancel_backend(560);

SELECT pg_terminate_backend(1268);


  select pg_terminate_backend(pid) from pg_stat_activity group by application_name, datname, pid having datname = 'vallia_docflow_dev' and application_name = 'PostgreSQL JDBC Driver';
  
  select pid from pg_stat_activity group by application_name, datname, pid having datname = 'vallia_docflow_dev' and application_name = 'PostgreSQL JDBC Driver'
  
  alter table rule_result  add constraint uniqueRuleResults unique(tenant_rule_id, rule_batch_id );
  
 
 select de.*, re2.* from "Document_Extraction" de full outer join rule_entity re2  on
 re2.doc_extraction_id  =de.id order by de.id
 	where de.created_at > now() - interval '30 minutes' 
 	
 	
 	select max(id) from rule_batch rb where rb.loan_number = '6010245831' and rb.tenant_id = 11;
 	
  
 
 select count(*), rr.run_status, rb.loan_number, rr.rule_batch_id from rule_result rr inner join 
 	rule_batch rb on 
 	rb.id  = rr.rule_batch_id 
  group by rb.loan_number , rr.run_status , rr.rule_batch_id 
  order by rr.rule_batch_id 
  
  
  truncate rule_result ;
  
 select * from task_pretty tp ;
 

select * from "Document_Extraction" 


select * from task te where te.sequence > 291

select de.document_type,de.file_name, task.state, task."output" from "Document_Extraction" de inner join task on 
 task.identifier::numeric = de.id and de.id > 250
 
 
 update rule_result set run_status = null where rule_batch_id = 21;
 

  SELECT blocked_locks.pid     AS blocked_pid,
         blocked_activity.usename  AS blocked_user,
         blocking_locks.pid     AS blocking_pid,
         blocking_activity.usename AS blocking_user,
         blocked_activity.query    AS blocked_statement,
         blocking_activity.query   AS current_statement_in_blocking_process
   FROM  pg_catalog.pg_locks         blocked_locks
    JOIN pg_catalog.pg_stat_activity blocked_activity  ON blocked_activity.pid = blocked_locks.pid
    JOIN pg_catalog.pg_locks         blocking_locks 
        ON blocking_locks.locktype = blocked_locks.locktype
        AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
        AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
        AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
        AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
        AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
        AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
        AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
        AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
        AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
        AND blocking_locks.pid != blocked_locks.pid

    JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
   WHERE NOT blocked_locks.granted;
   
  
  SHOW config_file
  
  SHOW all
  
  
  show shared_buffers
  
  show effective_cache_size 
  
  
 select * from task where task.created  > now() - interval  '40 minutes'
 
 

select t.state, t.output from document_metadata dm inner join "Document_Processing_Results" dpr on 
 dpr.metadata_id = dm.id 
 inner join "Document_Extraction" de on 
 de.doc_process_id = dpr.id 
 inner join task t on 
 t.identifier::int = de.id 
where dm.document_check_sum is not null  and de.extracted_json_data  is null and t.topic = 'EXTRACT_DOC_TYPE' and dm.document_id = 'UD-Gl-Xb9D6BWN9GwsL9q';


select de.* from document_metadata dm inner join "Document_Processing_Results" dpr on 
 dpr.metadata_id = dm.id 
 inner join "Document_Extraction" de on 
 de.doc_process_id = dpr.id 
 inner join task_pretty t on 
 t.identifier::int = de.id  
where dm.run_id = 'XMYrBmXJLXgVjjYLt-cTO' and t.topic = 'EXTRACT_DOC_TYPE' 


select t.state, de.file_name, de.document_type, de.created_at, dm.file_name, t."output", de.file_name from document_metadata dm inner join "Document_Processing_Results" dpr on 
 dpr.metadata_id = dm.id 
 inner join "Document_Extraction" de on 
 de.doc_process_id = dpr.id 
 inner join task t on 
 t.identifier::int = de.id 
where   
t.topic = 'EXTRACT_DOC_TYPE' and dm.document_id = 'XMYrBmXJLXgVjjYLt-cTO';


group by t.state , de.file_name, de.document_type , de.created_at, dm.file_name, t."output" ;


select count(*), de.document_type from document_metadata dm inner join "Document_Processing_Results" dpr on 
 dpr.metadata_id = dm.id 
 inner join "Document_Extraction" de on 
 de.doc_process_id = dpr.id 
 inner join task t on 
 t.identifier::int = de.id 
where dm.document_check_sum is not null  and t.topic = 'EXTRACT_DOC_TYPE' and t.state = 6 group by de.document_type ;

update document_metadata  set document_id = null where document_id ='cJxb4Ht7UrjfJZ7KdEGF4'



insert into tenant_settings (tenant_id, category,strategy, meta) values (11, 'upload_doc_cx_dx_checksum', 'run_always', null);



select count(*), tp.state, tp.topic from task_pretty tp where tp."sequence" in (
SELECT unnest((array_remove (array_agg(DISTINCT t), '__END__')::int[]))
    FROM task_tree AS f1,
         unnest((string_to_array(levels::text, '.')::text[])[3:]) AS t
    WHERE NOT EXISTS (
        SELECT 1
        FROM task_tree AS f2
        WHERE f1.levels <> f2.levels
          AND f1.levels @> f2.levels
          AND f2.qualifier in  (select  tt.qualifier from task_tree tt  where tt.meta ->> 'runId' = 'zAodOultKBE6XtuwQejGi')
          AND nlevel(f2.levels) > 2
    )
      AND f1.qualifier in ( select  tt.qualifier from task_tree tt  where tt.meta ->> 'runId' = 'zAodOultKBE6XtuwQejGi')
      AND nlevel(f1.levels) > 2
) group by tp.state , tp.topic;
      

 
 select * from task_tree tt where tt.qualifier = 'qPj4ZV7iu6VjQvS1ae3g2';
 
select * from document_metadata dm  where dm.document_id = 'qPj4ZV7iu6VjQvS1ae3g2';


select 
	sum(extract ('milliseconds' from completed -created)) as total_elapsed_time ,
	sum((convert_to_jsonb(tp.output) #>> '{internals, processTime}')::int) as process_time,
	count(*) as num_tasks,
	unnest((string_to_array(input, ','))[1:1]) as parent
--	tp.sequence
from task_pretty tp where tp."sequence" in (
	select subpath(tt.levels, nlevel(tt.levels)-1)::text::int from task_tree tt  
		where tt.qualifier  in (
		select tt.qualifier  from task_tree tt where 
  tt.qualifier  ^@ 'SIMPLE_LOAD_TEST' and nlevel(tt.levels) =1 and meta #>> '{runid}'  is not null
		) and 
		nlevel(tt.levels) > 2 and tt.levels  ~ '*.!__END__'
) group by  	unnest((string_to_array(input, ','))[1:1]);

--
with tree_with_run_id as (
	select tt.qualifier, meta #>> '{runId}'   as runid, meta #>> '{tenantId}'   as tenantId, tt.created_at 
		from task_tree tt where 
		   tt.qualifier  ^@ 'SIMPLE_LOAD_TEST' and 
		   nlevel(tt.levels) =1 and meta #>> '{runId}'  is not null 
), task_with_run_ids as (
	select subpath(tt.levels, nlevel(tt.levels)-1)::text::int as "sequence", 
	tree_with_run_id.runid,
	tree_with_run_id.tenantId,
	tt.qualifier  as qualifier,
	tree_with_run_id.created_at
	from task_tree tt  inner join tree_with_run_id on 
		tree_with_run_id.qualifier = tt.qualifier  
		where  
		nlevel(tt.levels) > 2 and tt.levels  ~ '*.!__END__'
), results as (
select 
	min(created) as min_created,
	max(completed) as max_completed,
	sum(extract ('milliseconds' from completed -created)) as total_elapsed_time ,
    avg(extract ('milliseconds' from completed -created)) as avg_elapsed_time ,
    sum(
    	try_cast(convert_to_jsonb(tp.output) #>> '{internals, execution, startTime}', null::timestamp) -
    	created
    	) as total_wait_time,
    avg(
    	try_cast(convert_to_jsonb(tp.output) #>> '{internals, execution, startTime}', null::timestamp) -
    	created
    	) as average_wait_time,
	sum((convert_to_jsonb(tp.output) #>> '{internals, execution, processTime}')::int) as total_process_time,
    avg((convert_to_jsonb(tp.output) #>> '{internals, execution, processTime}')::int) as avg_process_time,
	count(*) as num_tasks,
	trid.runid,trid.tenantId,
	unnest((string_to_array(input, ','))[1:1]) as qualifier,
	trid.created_at
from task_pretty tp 
inner join task_with_run_ids trid on 
trid."sequence" = tp."sequence"
where convert_to_jsonb(tp.input) #>> '{qualifier}' = trid.qualifier 
group by  	unnest((string_to_array(input, ','))[1:1]), trid.created_at, trid.runid,trid.tenantId
having 'SUCCEEDED' = all(array_agg(tp.state))
)
select  *, total_elapsed_time - total_process_time as wait_time_2 , 
avg_elapsed_time - avg_process_time as avg_wait_time, extract ('milliseconds' from  max_completed - min_created) as job_time  from results order by created_at desc;
--

 create or replace view task_with_runids as (
 with tree_with_run_id as (
	select tt.qualifier, meta #>> '{runId}'   as runid, meta #>> '{tenantId}'   as tenantId, tt.created_at, tt.meta
		from task_tree tt where
		   nlevel(tt.levels) =1 and meta #>> '{runId}'  is not null 
), task_with_run_ids as (
	select subpath(tt.levels, nlevel(tt.levels)-1)::text::int as trid_sequence, 
	tree_with_run_id.runid,
	tree_with_run_id.tenantId,
	tt.qualifier  as tree_qualifier,
	tree_with_run_id.created_at,
	tree_with_run_id.meta
	from task_tree tt  inner join tree_with_run_id on 
		tree_with_run_id.qualifier = tt.qualifier  
		where  
		nlevel(tt.levels) > 2 and tt.levels  ~ '*.!__END__'
) 
 select  * from task_pretty tp inner join task_with_run_ids trid on
 	trid.trid_sequence = tp."sequence"
);
 

drop view task_with_runids ;


select date_trunc('second', completed) as second_bin, runId , topic, tenantId, count(*) from task_with_runids group by tenantId, topic, second_bin, runId order by second_bin desc;





,
	full_name varchar(255) NULL,
	email_address varchar(255) NULL,


insert into tenant_settings (meta, category, strategy, tenant_id)
values ('"a698de9c-3167-4ee4-a695-79214e7bf83d"', 'auth', 'api_key', 11);

truncate hooks;


select * from "Document_Extraction" de where de.document_type = 'urla';

  
  -- public.rule_audit definition

-- Drop table

-- DROP TABLE public.rule_audit;

select * from hooks;

with configured_routes as (
select (json_array_elements(meta) #>> '{action, actionArgs, routeName}') as routeName from tenant_settings where strategy = 'task_route' and tenant_id = 11
), route_steps as (
select *, jsonb_array_elements(meta) ?| array['notificationTopic'] as has_topic   
	from task_tree tt 
	inner join configured_routes 
	on tr."name" = configured_routes.routeName
	where meta #>> tenantId = '11'
)
select * from route_steps where has_topic  


with configured_hooks as (
	select hook_type from hooks ts where tenant_id = 11
), tree_route_steps as (
select *, jsonb_array_elements(meta #> '{route, config}') as config    
	from task_tree tt
	where meta #>> '{tenantId}' = '11'
)
select *, s.config #>> '{notificationTopic}' as notificationTopic from tree_route_steps s inner join 
	configured_hooks h on
	s.config #>> '{notificationTopic}' = h.hook_type;
 
select * from task 
where 
	convert_to_jsonb(input) #>> '{notificationTopic}' in (select hook_type from hooks ts where tenant_id = 11) 
	and	topic not in ('INTERNAL_PUSH_HOOKS', 'INVOKE_HTTP')
	and created between '2025-03-01' and now()
	and convert_to_jsonb(input) #>> '{notificationTopic}'  ='after_extract'
order by created  desc;

with configured_routes as (
	select (json_array_elements(meta) #>> '{action, actionArgs, routeName}') as routeName from tenant_settings where strategy = 'task_route' and tenant_id = 11
), route_steps as (
select *, jsonb_array_elements(meta) ?| array['notificationTopic'] as has_topic   
	from task_tree tt 
	inner join configured_routes 
	on tr."name" = configured_routes.routeName
	where meta #>> tenantId = '11'
)


select * from hooks;


select t.* from task_pretty t
where
	convert_to_jsonb(input) #>> '{notificationTopic}' in (select hook_type from hooks ts where tenant_id = 11)
	and	topic not in ('INTERNAL_PUSH_HOOKS', 'INVOKE_HTTP')
	and created between :from and :to
order by created  desc

-- Classification           
 with doc_confidence as 
 (select *, json_array_elements(extracted_json_data -> 'documents') as conf_data from "Document_Processing_Results" dpr)
 select count(*) from doc_confidence dc where (dc.conf_data #>> '{confidence}')::numeric < 0.7 group by id 
 
 select  id, extracted_json_data , jsonb_path_query(extracted_json_data  ,  'strict $.**.confidence < 0.7') from "Document_Extraction" de ; 


select * from task_tree tt order by tt.id desc;

with dex_ids as (
select *, subpath(levels, 5, 6)::text::int   as did from task_tree tt 
where qualifier  in ('ySU8wTuFWnEDW8a4JeGje')
and nlevel(levels)= 6
order by  id desc, qualifier , created_at  desc 
) 
, childs as (
select sequence , input_json #>> '{creationArgs, docExId}' as did, input, output, state from task_pretty tp inner join dex_ids on tp."sequence" =  dex_ids.did 
-- topic, sequence ,  count(*), state, output group by tp.state, tp."output" , topic, sequence ;
) 
select * from "Document_Extraction" de inner join childs on childs.did = de.id::text
;

select * from "Document_Extraction" de where de.file_name like '%7bTEAG0Ssa.pdf_1_to_55.pdf%'

select * from document_metadata dm where dm.tenant_id = 9 order by id desc limit 5;

update task set state = 3 where "sequence" in (5335,5334);


//Bish_Rv
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('invoice', 'vallia_ci_rv_invoice_v1.0.0', 1, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('appone_quote', 'vallia_ci_rv_appone_quote_p1_v1.1.0', 2, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('appone_quote', 'vallia_ci_rv_appone_quote_p2_v1.1.0', 19, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('agreement_to_provide_insurance', 'vallia_ci_rv_agreement_to_provide_insurance_v1.0.0', 3, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('audit_calculation', NULL, 4, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('credit_application', 'vallia_ci_rv_credit_application_v1.0.0', 5, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('credit_decision', 'vallia_ci_rv_credit_decison_p1_v1.0.0', 6, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('credit_decision', 'vallia_ci_rv_credit_decision_p2_v1.0.0', 18, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('credit_score_disclosure', 'vallia_ci_rv_credit_score_disclosure_v1.0.0', 7, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('msrp_sticker', 'vallia_ci_rv_msrp_sticker_v1.0.0', 8, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('notice_of_adverse_action', NULL, 9, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('privacy_policy', 'vallia_ci_rv_privacy_policy_v1.0.0', 10, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('purchase_agreement', 'vallia_rv_purchase_agreeemnt_v1.0.1', 11, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('additional_terms_and_conditions', NULL, 12, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('service_contract_declaration_schedule', 'vallia_ci_rv_service_contract_declaration_schedule_v1.0.0', 13, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('retail_installment_contract_and_security_agreement', 'vallia_ci_rv_installment_contract_and_security_agreement_v1.0.0', 14, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('signature_notices', 'vallia_ci_rv_signature_notices_v1.0.0', 15, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('additional_protections', 'vallia_ci_rv_additional_protections_v1.0.0', 16, 'bish_rv');
INSERT INTO public.content_understanding_analyzers
(document_type, analyzer_id, id, profile)
VALUES('itemization_of_amount_financed', 'vallia_ci_rv_itemization_of_amount_financed_v1.0.0', 17, 'bish_rv');