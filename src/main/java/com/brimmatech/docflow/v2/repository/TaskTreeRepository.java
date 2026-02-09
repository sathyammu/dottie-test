package com.brimmatech.docflow.v2.repository;

import com.brimmatech.docflow.v2.dto.jpa.TaskAndTreeDto;
import com.brimmatech.docflow.v2.models.TaskTree;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface

TaskTreeRepository extends JpaRepository<TaskTree, Integer> {

  @Query(value = "SELECT meta->'route'->>'name' " +
          "FROM task_tree " +
          "WHERE qualifier = :qualifier AND levels = 'S'",
          nativeQuery = true)
  String findRouteNames(@Param("qualifier") String qualifier);

  @Query(value = """
    SELECT tp.input::jsonb ->> 'qualifier' AS qualifier
    FROM "Document_Extraction" de
    JOIN task_pretty tp
      ON (jsonb_extract_path_text(tp.input::jsonb, 'creationArgs', 'docExId'))::int = de.id
    WHERE de.loan_number = :loanNumber
      AND tp.topic = 'EXTRACT_DOC_TYPE'
      AND tp.input ~ '^{'
    LIMIT 1
    """, nativeQuery = true)
  String findQualifierByLoanNumber(@Param("loanNumber") String loanNumber);

  @Query(value = """
        select * from task_tree tt where tt.qualifier = :qualifier and nlevel(levels) = 1
      """, nativeQuery = true)
  Optional<TaskTree> findRootByQualifier(String qualifier);

   Optional<TaskTree> findByLevelsAndQualifier(String levels, String qualifier);

  @Query(value = """
        select * from task_tree tt where tt.qualifier = :qualifier and nlevel(levels) = 2
      """, nativeQuery = true)
  Optional<TaskTree> findSecondRootByQualifier(String qualifier);

  //findFirstLevels

  @Query(value = """
      SELECT unnest((array_remove (array_agg(DISTINCT t), '__END__')::int[]))
      FROM task_tree AS f1,
           unnest((string_to_array(levels::text, '.')::text[])[3:]) AS t
      WHERE NOT EXISTS (
          SELECT 1
          FROM task_tree AS f2
          WHERE f1.levels <> f2.levels
            AND f1.levels @> f2.levels
            AND f2.qualifier = :qualifier
            AND nlevel(f2.levels) > 2
      )
        AND f1.qualifier = :qualifier
        AND nlevel(f1.levels) > 2
      """, nativeQuery = true)
  List<Long> findTasks(@Param("qualifier") String qualifier);

  @Query(value = """
        with
        parent as (
          select
            tp.*,
            subpath (levels, -1),
            levels,
            nlevel (levels) depth
          from
            task_tree tt
            inner join task_pretty tp on tp.sequence = subpath (levels, -1)::text::bigint
            and tp.identifier = :supervisorIdentifier
          where
            tt.qualifier = :treeQualifier
            and meta #>> '{topic}' = 'NOOP_SUPERVISOR'
        ),
        siblings as (
          select
            tt.levels
          from
            task_tree tt
            inner join parent on nlevel (tt.levels) = parent.depth
          where
            tt.qualifier = :treeQualifier
            and not meta #>> '{topic}' = 'NOOP_SUPERVISOR'
        )
      select
            tt.id,
            tt.qualifier,
            tt.levels::text,
            nlevel(tt.levels)-2 as num_levels,
            tt.meta,
            tp.topic,
            tp.sequence,
            tp.identifier,
            tp.created,
            tp.completed,
            tp.state,
            tp.recreated,
            tp.input,
            tp.output
      from
        task_tree tt
        inner join siblings sb on sb.levels @> tt.levels
        inner join task_pretty tp on tp.sequence = subpath (tt.levels, -1)::text::bigint
      where
        not exists (
          select
            *
          from
            task_tree tt2
          WHERE
            tt.levels @> tt2.levels
            AND tt.levels <> tt2.levels
        )

            """, nativeQuery = true)
  List<TaskAndTreeDto> findLeavesWatchedBySupervisor(
      @Param("supervisorIdentifier") String supervisorIdentifier,
      @Param("treeQualifier") String treeQualifier);

  @Query(value = """
    SELECT COUNT(*)
    FROM task_tree tt
    JOIN document_metadata dm ON dm.run_id = tt.qualifier
    WHERE dm.tenant_id = :tenantId
      AND dm.change_ledger_id IS NULL
      AND nlevel(tt.levels) = 1
      AND tt.meta->'route'->>'name' IN ('PURCHASE_ADVICE', 'PURCHASE_CONDITION')
    """, nativeQuery = true)
  long countFilteredDocuments(@Param("tenantId") Long tenantId);

  @Query(value="""
  select tt.* 
    from task_tree tt
    inner join task_pretty tp 
    on tp.input_json #>> '{qualifier}' = tt.qualifier
    where (tp.input_json #>> '{creationArgs, orderId}') ::bigint = :titleOrderId 
    and nlevel(tt.levels) =1 
    order by tt.created_at desc limit 1  
""", nativeQuery = true)
  Optional<TaskTree> getTreeForTitleOrder(long titleOrderId);


    @Query(value = "SELECT t.qualifier FROM task_tree t ORDER BY t.id DESC LIMIT 1", nativeQuery = true)
    String findMostRecentQualifier();


    @Query(value = """
        SELECT 
            tt.qualifier AS qualifier,
            tt.meta -> 'route' ->> 'name' AS route_name
        FROM task_tree tt
        WHERE tt.levels ~ 'S'
          AND DATE(tt.created_at) = :createdDate
          AND (tt.meta ->> 'tenantId')::int = :tenantId
        """, nativeQuery = true)
    List<Map<String, Object>> findTasksByDateAndTenant(
            @Param("createdDate") LocalDate createdDate,
            @Param("tenantId") int tenantId
    );


    @Query(value = """
        SELECT 
            tt.qualifier AS qualifier,
            tt.meta -> 'route' ->> 'name' AS route_name
        FROM task_tree tt
        WHERE tt.levels ~ 'S'
          AND DATE(tt.created_at) = :createdDate
          AND (tt.meta ->> 'tenantId')::int = :tenantId
          AND tt.meta -> 'route' ->> 'name' = :route
        """, nativeQuery = true)
    List<Map<String, Object>> findTasksByDateAndRoute(
            @Param("createdDate") LocalDate createdDate,
            @Param("tenantId") int tenantId,
            @Param("route") String route
    );


    @Query(value = """
        SELECT DISTINCT tt.meta -> 'route' ->> 'name' AS route_name
        FROM task_tree tt
        WHERE tt.levels ~ 'S'
          AND (tt.meta ->> 'tenantId')::int = :tenantId
        """, nativeQuery = true)
    List<String> findDistinctRoutesByTenant(@Param("tenantId") long tenantId);

  @Query(value = """
    select (subpath(tt.levels, nlevel(tt.levels) - 1, 1))::text::bigint
    from task_tree tt
    where tt.qualifier = :qualifier
      and tt.meta ->> 'topic' = :topic
    order by
        nlevel(tt.levels) desc,
        tt.created_at desc
    limit 1
""", nativeQuery = true)
  Optional<Long> findLatestDeepestLastLevelByQualifierAndTopic(
          @Param("qualifier") String qualifier,
          @Param("topic") String topic
  );

  @Query(value = """
          SELECT tt.qualifier FROM task_tree tt
          WHERE Date(tt.created_at) > Date(:fromDate) and Date(tt.created_at) < Date(:toDate)
          and nlevel(tt.levels) = 1 and tt.meta ->> 'tenantId' =:tenantId
          and tt.meta -> 'route' ->> 'name' = 'ON_LOAN_MODIFIED_RULE_EXECUTION'
          """,nativeQuery = true)
  List<String> getAllQualifierForATenant(String fromDate,String toDate,String tenantId);

  @Modifying
  @Transactional
  @Query(value = """
          Delete FROM task_tree
          WHERE qualifier = :qualifier and meta ->>'topic' = 'RUN_RULES'
          """,nativeQuery = true)
  void deleteRunRulesTask(String qualifier);







}
