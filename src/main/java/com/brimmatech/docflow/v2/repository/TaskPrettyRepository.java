package com.brimmatech.docflow.v2.repository;

import com.brimmatech.docflow.v2.models.TaskPretty;
import com.brimmatech.general.config.TemplateConfig;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TaskPrettyRepository extends JpaRepository<TaskPretty, Integer> {

    Optional<TaskPretty> findBySequence(long sequence);

    List<TaskPretty> findBySequenceIn(List<Long> sequence);

    @Query(value = "select t.* from task_pretty t where t.input_json #>> '{qualifier}' = :qualifier ", nativeQuery = true)
    List<TaskPretty> findByQualifier(String qualifier);

    @Query(value = """
    select t.* from task_pretty t where t.input_json #>> '{qualifier}' = (
     select run_id from document_metadata dm where dm.document_id = :documentId)
    """, nativeQuery = true)
    List<TaskPretty> findByDocumentId(String documentId);

    @Query(value = """
            WITH RankedRows AS (
               SELECT
                   *,
                   ROW_NUMBER() OVER (PARTITION BY input_json #>> '{qualifier}'  ORDER BY created DESC) AS row_rank
               FROM task_pretty
               WHERE input_json #>> '{qualifier}'  IN :qualifiers
           )
           SELECT *
           FROM RankedRows
           WHERE row_rank = 1 order by created desc;
            """, nativeQuery = true)
    List<TaskPretty> findRunningTaskByQualifierIn(List<String> qualifiers);

    List<TaskPretty> findBySequenceInAndTopic(List<Long> sequence, TemplateConfig.TOPICS topic);

    List<TaskPretty> findBySequenceInAndTopicAndState(List<Long> sequence, TemplateConfig.TOPICS topic, TaskState state);

    TaskPretty findByIdentifier(String identifier);

    List<TaskPretty> findBySequenceInOrderByCreatedAsc(List<Integer> sequence);

    @Query(value = """
            select distinct on (t.identifier) * from task_pretty t where t.identifier in
            :identifiers order by t.identifier, t.sequence desc
            """, nativeQuery = true)
    List<TaskPretty> findResultsForRuleBatch(List<String> identifiers);

	@Query(value = """
            select * from task_pretty tp where
                (input_json #>> '{tenantId}')::int = :tenantId and
                    tp.topic = 'EXTRACT_DOC_TYPE' and
                    tp.state = 'FAILED' and
                    created::DATE = :date and
                    (
                        (tp.output ilike '%doc notfound%' or tp.output = 'ERR_EXTRACTION_MODEL_NOT_FOUND')
                        or
                        (convert_to_jsonb(tp.output)  #>> '{internals,executions,-1,errorCode}' = 'ERR_EXTRACTION_MODEL_NOT_FOUND')
                    )
            """, nativeQuery = true)
	List<TaskPretty> getFailedExtractionTasks(long tenantId, LocalDate date);

    @Query(value = """
    select * from task_pretty t where
     t.input_json #>> '{qualifier}' = :qualifier and t.topic = :topic
    """, nativeQuery = true)
    Optional<TaskPretty> findMatchingQualifierAndTopic(String qualifier, String topic);


    @Query(value = """            
            select tp.* from task_pretty tp
            inner join notifications n on n.task_sequence = tp.sequence
            where n.task_sequence in :sequences 
            order by created asc 
            """, nativeQuery = true)
    List<TaskPretty> findTaskPerNotification(List<Integer> sequences);

    @Query(value = """
                              with parent as (
                              select
                              	qualifier,
                              	(case
                              		when tt.meta #>> '{route, name}' is null then 
                              		    concat('topic_' , tt.meta #>> '{route, config, 0, topic}' )
                              		else 
                              		    tt.meta #>> '{route, name}'
                              	end) as route
                              from
                              	task_tree tt
                              where
                              	nlevel(levels) = 1
                               ),
                              middle_nodes as (
                              select
                              	tp.*,
                              	tt.qualifier,
                              	subpath (levels,-1),
                              	levels,
                              	nlevel (levels) depth,
                              	(input_json #>> '{tenantId}')::int as tenantId,
                              	parent.route
                              from
                              	task_tree tt
                              inner join task_pretty tp on
                              	tp.sequence = subpath (levels,-1)::text::bigint
                              inner join parent on
                              	tt.qualifier = parent.qualifier
                              where
                              	subpath (levels,-1) != '__END__'
                              		and
                                       nlevel(levels)>2
                              and
                                   created between  :from  and :to and
                                    (input_json #>> '{tenantId}')::int = :tenantId)
                                  SELECT topic, sequence, identifier, created, completed, input, output, qualifier, tenantId, levels::text,depth, route, state,  subpath(levels, 1,1)::text as tree_root
                              	FROM (
                              	  SELECT *,
                              	    ROW_NUMBER() OVER (PARTITION BY qualifier ORDER BY depth DESC) AS rn
                              	  FROM middle_nodes
                              	) t
                              	WHERE rn = 1 order by sequence  desc;
            """, nativeQuery = true)
    List<ReportTaskAndTree> getTasksForReport(Date from, Date to, long tenantId);

    @Query(value = """
            select t.* from task_pretty t
            where
            	input_json #>> '{notificationTopic}' in (select hook_type from hooks ts where tenant_id = :tenantId and hook_type = :eventType)
            	and	topic not in ('INTERNAL_PUSH_HOOKS')
            	and created between :from and :to
            order by created  desc
            """,
            nativeQuery = true,
            countQuery = """
                    select count(t.*) from task_pretty t
                    where
                    	input_json #>> '{notificationTopic}' in (select hook_type from hooks ts where tenant_id = :tenantId and hook_type = :eventType)
                    	and	topic not in ('INTERNAL_PUSH_HOOKS')
                    	and created between :from and :to
                    """)
    org.springframework.data.domain.Page<TaskPretty> _getAllNotifiedTasks(String eventType, ZonedDateTime from, ZonedDateTime to, long tenantId, Pageable pageable);



    @Query(value = """
        WITH valid_data AS (
            SELECT
                "input"::jsonb AS input_json,
                "output"::jsonb AS output_json
            FROM task_pretty
            WHERE "input" ~ '^[\\{\\[].*[\\}\\]]$'
              AND "output" ~ '^[\\{\\[].*[\\}\\]]$'
        ),
        last_exec_per_task AS (
            SELECT
                vd.input_json ->> 'qualifier' AS qualifier,
                (exec_elem ->> 'processTime')::int AS process_time
            FROM valid_data vd
            CROSS JOIN LATERAL (
                SELECT exec_elem
                FROM jsonb_array_elements(vd.output_json -> 'internals' -> 'executions')
                     WITH ORDINALITY arr(exec_elem, idx)
                ORDER BY idx DESC
                LIMIT 1
            ) sub
        )
        SELECT
            qualifier,
            ROUND(SUM(process_time) / 60000.0, 2) AS total_process_time_minutes
        FROM last_exec_per_task
        WHERE qualifier = :qualifier
        GROUP BY qualifier
        """, nativeQuery = true)
    Map<String, Object> findTotalProcessTimeByQualifier(@Param("qualifier") String qualifier);


    @Query(value = """
    SELECT 
        (tp.input::jsonb -> 'creationArgs' ->> 'docExId')::bigint AS docExId,
        tp.state AS state
    FROM task_pretty tp
    WHERE tp.input IS NOT NULL
      AND tp.input ~ '^\\s*\\{.*\\}\\s*$'
      AND tp.sequence = :sequence
    """, nativeQuery = true)
    List<Map<String, Object>> findDocExIdsWithStateMap(@Param("sequence") Long sequence);

    @Query(value = """
    SELECT
        (tp.input::jsonb ->> 'runId') AS runId
    FROM task_pretty tp
    WHERE
        tp.input IS NOT NULL
        AND pg_input_is_valid(tp.input, 'json')
        AND jsonb_typeof(tp.input::jsonb) = 'object'
        AND (tp.input::jsonb #>> '{creationArgs,processingResultModelId}')::bigint
            = :processingResultModelId
    """, nativeQuery = true)
    String findRunIdsByProcessingResultModelId(
            @Param("processingResultModelId") Integer processingResultModelId
    );

    @Query(value = """
    SELECT 
        tp.input::jsonb ->> 'qualifier' AS qualifier
    FROM task_pretty tp
    WHERE tp.input ~ '^\\s*\\{.*\\}\\s*$'
      AND (tp.input::jsonb -> 'tenantId')::int = :tenantId
    ORDER BY tp.created DESC
    LIMIT 1
    """, nativeQuery = true)
    String findRecentQualifierByTenant(@Param("tenantId") Long tenantId);


    @Query(value = """
    SELECT 
        *
    FROM task_pretty tp
    WHERE tp.input_json #>> '{creationArgs,requestedAction}' = :genieAction and 
         (tp.input_json #>> '{creationArgs,orderId}')::int = :orderId
    """, nativeQuery = true)
    Optional<TaskPretty> findOrderCreationTask(String genieAction, long orderId);

    @Query(value = """
    SELECT t.input_json -> 'creationArgs' ->> 'paUpdatedMetaId'
    FROM task_pretty t
    WHERE t.sequence = :sequence
      AND t.meta::text LIKE '%PURCHASE_ADVICE_EXTRACT%'
""", nativeQuery = true)
    String findPaUpdatedMetaIdBySequence(@Param("sequence") Integer sequence);

    List<TaskPretty> findByTopicOrderBySequenceDesc( TemplateConfig.TOPICS topic);





}
