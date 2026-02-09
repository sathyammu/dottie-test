with times as (
select (convert_to_jsonb(output) #>> '{internals, executions, 0, processTime}')::int as duration from task_pretty where input_json #>> '{creationArgs, strategy}' = 'CONTENT_UNDERSTANDING'
and state = 'SUCCEEDED' and topic = 'EXTRACT_DOC_TYPE'
and (convert_to_jsonb(output) #>> '{internals, executions, 0, processTime}')::int is not  null
), pc  as (
select duration  ,
    NTILE(10) OVER (ORDER BY duration) AS ntiled,
    COUNT(*) OVER () AS counted
    from times
  )
  select avg(duration), count(*), stddev(duration),  ntiled , mode() WITHIN GROUP (ORDER BY duration) from pc group by ntiled;
--  select avg(duration) from pc where counted < 10 or ntiled between 2 and 9;


with times as (
select (convert_to_jsonb(output) #>> '{internals, executions, 0, processTime}')::int as duration from task_pretty where
(input_json #>> '{creationArgs, strategy}' is null or input_json #>> '{creationArgs, strategy}' = 'DOC_INTEL')
and state = 'SUCCEEDED' and topic = 'EXTRACT_DOC_TYPE'
and (convert_to_jsonb(output) #>> '{internals, executions, 0, processTime}')::int is not  null
), pc  as (
select duration  ,
    NTILE(10) OVER (ORDER BY duration) AS ntiled,
    COUNT(*) OVER () AS counted
    from times
  )
  select avg(duration), count(*), stddev(duration),  ntiled , mode() WITHIN GROUP (ORDER BY duration) from pc group by ntiled;