delete from task_tree where created_at is null;

select * from task_tree tt order by tt.created_at desc;

select now();

with parent as (
select
	qualifier,
	created_at,
	(case
		when tt.meta #>> '{route, name}' is null then concat('topic_' ,
		tt.meta #>> '{route, config, 0, topic}' )
		else tt.meta #>> '{route, name}'
	end) as route
from
	task_tree tt
where
	nlevel(levels) = 1 	
	and created_at > '2025-03-25'::DATE 
    and (meta #>> '{tenantId}')::int = 2
order by created_at desc
limit 1
 ) ,
middle_nodes as (
select
	tp.*,
	tt.qualifier,
	subpath (levels,
	-1),
	levels,
	nlevel (levels) depth,
	(input_json #>> '{tenantId}')::int as tenantId,
	parent.route
from
	task_tree tt
inner join task_pretty tp
                                         on
	tp.sequence = subpath (levels,
	-1)::text::bigint
inner join parent on
	tt.qualifier = parent.qualifier
where
	subpath (levels,
	-1) != '__END__'		and         nlevel(levels)>2 
)
SELECT qualifier, subpath(levels, 1,1)::text as tree_root, topic,  state, sequence, created, completed, input, output, tenantId, levels::text,depth, route
from middle_nodes order by sequence desc
--	FROM (
--	  SELECT max(depth) as depth,
--	    ROW_NUMBER() OVER (PARTITION BY qualifier ORDER BY depth DESC) AS rn
--	  FROM middle_nodes
--	  group by depth 
--	) t
--	WHERE depth = t.depth order by sequence  desc;


select * from "Document_Extraction" de  where de.id::text in (
	select input_json #>> '{creationArgs, docExId}' from task_pretty tp where tp.sequence in (
	11185,
11184,
11150,
11176,
11181,
11188,
11173,
11183)
	
)

select * from task_tree tt where tt.qualifier = 'yWifJkaVCkyMDeARtWOoX'

select * from task_pretty tp where tp.input_json #>> '{qualifier}' ='wEeHviLTA_0FPvcGTBcZw'


select * from hooks;

select * from task_pretty tp where input_json #>> '{tenantId}' = '18' and topic = 'INTERNAL_PUSH_HOOKS'