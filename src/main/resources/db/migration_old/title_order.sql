select * from title_order to2 order by to2.created_at desc;

select tio.*, (input_json #>> '{creationArgs, orderId}')::int as orderId, * from task_pretty inner join title_order tio on tio.id =  (input_json #>> '{creationArgs, orderId}')::int
where topic = 'SYNC_WITH_GENIE'
--	and state in ('FAILED', 'SUSPENDED')
--	and tio.sync_status != 'HTML_SCRAPE_ERROR'
 order by created desc ;

select * from task_pretty  where topic = 'SYNC_WITH_GENIE'
--and state  in (3,4)
-- and (input_json #>> '{creationArgs, orderId}')::int = 22
 order by created desc ;

select * from title_order to2 where to2.id = 85;
select * from task where "sequence"  = 11339;


select * from task_pretty where (input_json #>> '{creationArgs, orderId}')::int = 85 order by "sequence"  desc	;


select * from spring_ai_chat_memory sacm order by timestamp desc 	;

update task set state = 1 where sequence = 21779;
update title_order set sync_status = 'PENDING' where id=85;


select * from title_order to2 order by created_at desc limit 4;

select * from spring_ai_chat_memory sacm order by timestamp desc ;