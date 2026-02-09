package com.brimmatech.docflow.v2.task.repository;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.enums.SuspensionStrategy;
import com.brimmatech.docflow.v2.dto.TaskDto;
import com.brimmatech.docflow.v2.models.TaskPretty;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.docflow.v2.task.dto.CreationArgs.TaskInputArgs;
import com.brimmatech.docflow.v2.task.dto.EncompassUpdateDtos;
import com.brimmatech.docflow.v2.task.dto.TaskOutputArgs;
import com.brimmatech.docflow.v2.task.listeners.SupervisorWaker;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.brimmatech.general.infra.ConnectionProvider;
import com.brimmatech.general.types.Tuple;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.*;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.jdbc.PostgresTaskRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor @Slf4j
public class ConnectionProviderTaskRepository implements TaskRepository<ConnectionProvider, SQLException> {

    private final PostgresTaskRepository postgresTaskRepository;
    private final SupervisorWaker supervisorWaker;
    private final ObjectMapper objectMapper;
    private final TenantSettingsService tenantSettingsService;

    @Override
    public List<Task> push(ConnectionProvider transaction,
                           String topic,
                           Insertion insertion,
                           Collection<TaskCreation> creations) throws SQLException {
        log.debug("Creating task({}) with ids :{}",
                topic,
                creations.stream().map(TaskCreation::getIdentifier).collect(Collectors.joining()));
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.push(con, topic, insertion, creations);
        }
    }

    public void retryExtractionTasks(ConnectionProvider transaction,
                                     List<Tuple<Integer, CreationArgs.TaskInputArgs>> sequencesWithNewInputs) throws SQLException, JsonProcessingException {
        try (Connection conn = transaction.provideConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE TASK SET INPUT = ?, STATE = ?
                    WHERE SEQUENCE = ?
                    """)) {

                for (Tuple<Integer, TaskInputArgs> sequencesWithNewInput : sequencesWithNewInputs) {

                    ps.setString(1, objectMapper.writeValueAsString(sequencesWithNewInput.right));
                    ps.setInt(2, TaskState.READY.ordinal());
                    ps.setInt(3, sequencesWithNewInput.left);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    public void setZombiesAsReady(ConnectionProvider transaction) {
        try (Connection conn = transaction.provideConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE TASK SET STATE = ?
                    WHERE STATE = ?
                    AND created < NOW();
                    """)) {

                ps.setInt(1, TaskState.SUSPENDED.ordinal());
                ps.setInt(2, TaskState.ACTIVE.ordinal());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.debug("Unable to reactivate zombie tasks during startup", e);
        }
    }



    @Override
    public Set<Task> poll(ConnectionProvider transaction,
                          String topic,
                          Order order,
                          Condition condition,
                          int size) throws SQLException {
        try (Connection conn = transaction.provideConnection()) {

            boolean isCurrentTopicSupervisor = topic.equals(TOPICS.NOOP_SUPERVISOR.toString());
            val
                    suspensionStrategy =
                    tenantSettingsService.getGlobalSettingTyped(SettingsCategory.SUSPENSION_STRATEGY,
                                    TenantSettingsService.SuspensionStrategy.class)
                            .map(TenantSettingsService.SuspensionStrategy::strategy)
                            .orElse(SuspensionStrategy.SELF_SUSPENSION);

            switch (suspensionStrategy) {
                case SELF_SUSPENSION:
                    if (isCurrentTopicSupervisor) {
                        unhibernateInitialSupervisors(conn);
                    }
                    wakeSuspendedTasks(topic, conn);
                    break;

                case ACTIVATE_BY_CHILDREN:
                    if (!isCurrentTopicSupervisor) {
                        wakeSuspendedTasks(topic, conn);
                        unhibernateSupervisors(conn);
                    }
                    break;
            }


            if (size == 0) {
                return Set.of();
            }
            Set<Task> tasks = new LinkedHashSet<>();
            try (PreparedStatement ps = conn.prepareStatement("""
                    WITH POLLED (SEQUENCE) AS (
                    SELECT SEQUENCE FROM TASK WHERE SEQUENCE IN (
                        SELECT SEQUENCE FROM (
                                SELECT SEQUENCE, RANK() OVER
                                (PARTITION BY 
                                    INPUT_JSON #>> '{tenantId}' 
                                    ORDER BY 
                                        COALESCE((INPUT_JSON #>> '{priority}')::int, 0 ) DESC,
                                        created desc
                                ) AS U
                                FROM TASK WHERE TOPIC = ? AND STATE = ?
                                FETCH FIRST ? ROWS ONLY
                        ) as sequence_result
                    )
                    FOR UPDATE SKIP LOCKED )
                    UPDATE TASK SET STATE = ?, OWNER = ?
                    WHERE SEQUENCE IN (SELECT SEQUENCE FROM POLLED)
                    RETURNING SEQUENCE, IDENTIFIER, INPUT;
                    """)) {
                ps.setString(1, topic);
                ps.setInt(2, TaskState.READY.ordinal());
                ps.setInt(3, size);
                ps.setInt(4, TaskState.ACTIVE.ordinal());
                ps.setString(5, postgresTaskRepository.getOwner());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tasks.add(new Task(rs.getLong("SEQUENCE"), rs.getString("IDENTIFIER"), rs.getString("INPUT")));
                    }
                }
            }
            return tasks;
        }
    }

    private void unhibernateSupervisors(Connection conn) {
        val outputNode = objectMapper.createObjectNode().put("hasWokeUp", "true");
        ArrayList<String> wakeableCandidates = supervisorWaker.drainToList();
        if (wakeableCandidates.isEmpty()) {
            return;
        }
        log.info("Waking up supervisors with ids:{}", wakeableCandidates);

        try (PreparedStatement ps = conn.prepareStatement("""
                WITH POLLED (SEQUENCE) AS (
                SELECT	SEQUENCE FROM	TASK tp WHERE
                    TOPIC = ?  AND
                    TP.STATE = ?  AND
                    TP.identifier  = ANY(?)
                FOR	UPDATE SKIP LOCKED)
                UPDATE	TASK SET STATE = ?, OUTPUT = ?
                WHERE SEQUENCE IN (	SELECT SEQUENCE	FROM POLLED);
                """)) {
            ps.setString(1, TOPICS.NOOP_SUPERVISOR.toString());
            ps.setInt(2, TaskState.SUSPENDED.ordinal());
            ps.setArray(3, conn.createArrayOf("text", wakeableCandidates.toArray()));
            ps.setInt(4, TaskState.READY.ordinal());
            ps.setString(5, TaskOutputArgs.create().withTaskOutput(outputNode).toJsonString(objectMapper));
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Got Exception while unhibernating supervisor tasks: {}", wakeableCandidates, e);
        }
    }

    private void unhibernateInitialSupervisors(Connection conn) {
        val outputNode = objectMapper.createObjectNode().put("hasWokeUp", "true");
        ArrayList<String> wakeableCandidates = supervisorWaker.drainToList();
        if (wakeableCandidates.isEmpty()) {
            return;
        }
        log.info("Waking up supervisors with ids:{}", wakeableCandidates);

        try (PreparedStatement ps = conn.prepareStatement("""
                WITH POLLED (SEQUENCE) AS (
                SELECT	SEQUENCE FROM	TASK tp WHERE
                    TOPIC = ?  AND
                    TP.STATE = ?  AND
                    TP.identifier  = ANY(?)
                     AND TP.OUTPUT is NULL
                FOR	UPDATE SKIP LOCKED)
                UPDATE	TASK SET STATE = ?, OUTPUT = ?
                WHERE SEQUENCE IN (	SELECT SEQUENCE	FROM POLLED);
                """)) {
            ps.setString(1, TOPICS.NOOP_SUPERVISOR.toString());
            ps.setInt(2, TaskState.SUSPENDED.ordinal());
            ps.setArray(3, conn.createArrayOf("text", wakeableCandidates.toArray()));
            ps.setInt(4, TaskState.READY.ordinal());
            ps.setString(5, TaskOutputArgs.create().withTaskOutput(outputNode).toJsonString(objectMapper));
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Got Exception while unhibernating supervisor tasks: {}", wakeableCandidates, e);
        }
    }

    private void wakeSuspendedTasks(String topic, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("""
                WITH POLLED (SEQUENCE) AS (
                SELECT	SEQUENCE FROM	TASK tp WHERE
                    TOPIC = ?  AND
                    TP.STATE = ?  AND
                    (
                    try_cast(CONVERT_TO_JSONB(TP.OUTPUT) #>> '{internals,suspensions,-1,toBeActiveAt}', NULL::TIMESTAMP) < timezone('utc', now()) or 
                    CONVERT_TO_JSONB(OUTPUT) #> '{internals,suspensions}' = '[]'::jsonb  or 
                    CONVERT_TO_JSONB(OUTPUT) #> '{internals,suspensions}' is null
                    )
                
                FOR	UPDATE SKIP LOCKED)
                UPDATE	TASK SET STATE = ?
                WHERE SEQUENCE IN (	SELECT SEQUENCE	FROM POLLED);
                """)) {
            ps.setString(1, topic);
            ps.setInt(2, TaskState.SUSPENDED.ordinal());
            ps.setInt(3, TaskState.READY.ordinal());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Got Exception while waking suspended task with topic : {}", topic, e);
        }
    }

    public void updateInput(Task task, Connection conn, TaskInputArgs input) {
        try (PreparedStatement ps = conn.prepareStatement("""
                
                UPDATE	TASK SET INPUT = ?
                WHERE SEQUENCE = ?
                """)) {
            ps.setString(1, objectMapper.writeValueAsString(input));
            ps.setLong(2, task.getSequence());
            ps.executeUpdate();
        } catch (JsonProcessingException | SQLException e) {
            log.debug("Got Exception while updating input", e);
        }
    }

    public void updateOutput(Task task, Connection conn, TaskOutputArgs outputArgs) {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        
                        UPDATE	TASK SET OUTPUT = ?
                        WHERE SEQUENCE = ?
                        """)) {
            ps.setString(1, objectMapper.writeValueAsString(outputArgs));
            ps.setLong(2, task.getSequence());
            ps.executeUpdate();
        } catch (JsonProcessingException | SQLException e) {
            log.debug("Got Exception while updating input", e);
        }
    }

    public void updatePrettyInput(TaskPretty taskPretty, Connection conn, TaskInputArgs input) {
        try (PreparedStatement ps = conn.prepareStatement("""
                WITH POLLED (SEQUENCE) AS (
                SELECT	SEQUENCE FROM	TASK tp WHERE
                    TP.SEQUENCE = ?
                
                FOR	UPDATE SKIP LOCKED)
                UPDATE	TASK SET INPUT = ?, STATE = 1
                WHERE SEQUENCE IN (	SELECT SEQUENCE	FROM POLLED);
                """)) {
            ps.setLong(1, taskPretty.getSequence());
            ps.setString(2, objectMapper.writeValueAsString(input));
            ps.executeUpdate();
        } catch (JsonProcessingException | SQLException e) {
            log.debug("Got Exception while updating input", e);
        }
    }


    @Override
    public void complete(ConnectionProvider transaction,
                         String topic,
                         Map<Task, TaskDecision> decisions) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            postgresTaskRepository.complete(con, topic, decisions);
        }
    }

    @Override
    public Map<Task, Task> recreate(ConnectionProvider transaction,
                                    String topic,
                                    Revivification revivification,
                                    Revived revived,
                                    long from,
                                    long to,
                                    int size) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.recreate(con, topic, revivification, revived, from, to, size);
        }
    }

    @Override
    public Set<String> junction(ConnectionProvider transaction,
                                String topic,
                                Junction junction,
                                Collection<Set<String>> groups) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.junction(con, topic, junction, groups);
        }
    }

    @Override public boolean register(ConnectionProvider transaction) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.register(con);
        }
    }

    @Override
    public Set<String> owners(ConnectionProvider transaction, long timeout, TimeUnit unit) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.owners(con, timeout, unit);
        }
    }

    @Override public void heartbeat(ConnectionProvider transaction) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            postgresTaskRepository.heartbeat(con);
        }
    }

    @Override public void expire(ConnectionProvider transaction, long timeout, TimeUnit unit) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            postgresTaskRepository.expire(con, timeout, unit);
        }
    }

    @Override public boolean initialize(ConnectionProvider transaction, String topic) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.initialize(con, topic);
        }
    }

    @Override
    public void reassign(ConnectionProvider transaction,
                         String topic,
                         Map<Task, TaskResult> tasks) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            postgresTaskRepository.reassign(con, topic, tasks);
        }
    }

    @Override
    public long reassignAll(ConnectionProvider transaction,
                            String topic,
                            Revived revived,
                            TaskResult result,
                            long from,
                            long to) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.reassignAll(con, topic, revived, result);
        }
    }

    @Override
    public <TASK extends Task> Map<TASK, Task> recreate(ConnectionProvider transaction,
                                                        String topic,
                                                        Revivification revivification,
                                                        Set<TASK> tasks) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.recreate(con, topic, revivification, tasks);
        }
    }

    @Override
    public List<TaskInfo> page(ConnectionProvider transaction,
                               String topic,
                               Listing listing,
                               long sequence,
                               int size,
                               Direction direction) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.page(con, topic, listing, sequence, size);
        }
    }

    @Override public Set<String> topics(ConnectionProvider transaction) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.topics(con);
        }
    }

    @Override
    public Map<String, Map<TaskState, Summary>> count(ConnectionProvider transaction,
                                                      Snapshot snapshot,
                                                      Counting counting,
                                                      long from,
                                                      long to) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.count(con, snapshot, counting, from, to);
        }
    }

    @Override
    public Map<String, Map<TaskResult, Long>> results(ConnectionProvider transaction,
                                                      OffsetDateTime from,
                                                      OffsetDateTime to) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.results(con, from, to);
        }
    }

    @Override public boolean destroy(ConnectionProvider transaction, String topic) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.destroy(con, topic);
        }

    }

    @Override public void purgeAll(ConnectionProvider transaction, String topic) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            postgresTaskRepository.purgeAll(con, topic);
        }
    }

    @Override public long purge(ConnectionProvider transaction, String topic, long from, long to) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.purge(con, topic, from, to);
        }
    }

    @Override
    public long purge(ConnectionProvider transaction,
                      String topic,
                      TaskState state,
                      long from,
                      long to) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.purge(con, topic, state, from, to);
        }
    }

    @Override public int purgeOwners(ConnectionProvider transaction, long timeout, TimeUnit unit) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.purgeOwners(con, timeout, unit);
        }
    }

    @Override
    public long resolve(ConnectionProvider transaction,
                        OffsetDateTime dateTime,
                        boolean preceding) throws SQLException {
        try (Connection con = transaction.provideConnection()) {
            return postgresTaskRepository.resolve(con, dateTime, preceding);
        }
    }

    public int updateMissingFieldsEmpty(ConnectionProvider connectionProvider, Long runId) throws SQLException {
        String
                sql =
                "UPDATE task " +
                        "SET input = jsonb_set( " +
                        "    input::jsonb, '{creationArgs,missingfields}', '[]'::jsonb " +
                        ") " +
                        "WHERE sequence = ( " +
                        "    SELECT sequence " +
                        "    FROM ( " +
                        "        SELECT sequence, input " +
                        "        FROM task " +
                        "        WHERE topic = 'UPDATE_ENCOMPASS' " +
                        "          AND input LIKE '{%' " +
                        "          AND input NOT ILIKE '%errorMessage\":null,{%' " +
                        "    ) AS filtered " +
                        "    WHERE (input::jsonb -> 'creationArgs' -> 'taskInput' ->> 'docExId')::bigint = ? " +
                        "    ORDER BY input DESC " +
                        "    LIMIT 1 " +
                        ")";
        try (Connection conn = connectionProvider.provideConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, runId); // set runId as string
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated;
        } catch (Exception ex) {
            log.info(ex.getMessage());
            throw new SQLException("Failed to update missingfields using runId", ex);
        }
    }


    public void retryBackingTasks(ConnectionProvider connectionProvider, List<Long> orderIds) {
        //TODO replace this with "select for update skip locked"
        String
                sql = """                
                        UPDATE task
                        SET state = ?
                        WHERE topic = 'SYNC_WITH_GENIE' and
                        input_json #>> '{creationArgs, orderId}' = ANY (?) and
                        state NOT IN(?,?)
                """;

        try (Connection conn = connectionProvider.provideConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, TaskState.READY.ordinal());
            stmt.setArray(2, conn.createArrayOf("text", orderIds.toArray()));
            stmt.setInt(3, TaskState.ACTIVE.ordinal());
            stmt.setInt(4, TaskState.SUSPENDED.ordinal());
            log.debug("Executing statement:{}", stmt);
            stmt.executeUpdate();
        } catch (Exception ex) {
            log.error("Failed to retry fee mappings", ex);
        }
    }

    public int updateTaskStateBySequence(Connection conn, List<Integer> sequenceIds, TaskState state) {
        String
                sql =
                "UPDATE task " +
                        "SET state = ? " +
                        "WHERE sequence = ANY (?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, state.ordinal());
            stmt.setArray(2, conn.createArrayOf("bigint", sequenceIds.toArray()));
            log.debug("Executing statement:{}", stmt);
            return stmt.executeUpdate();
        } catch (Exception ex) {
            log.error("Failed ", ex);
            return 0;
        }
    }

    public void restartGenieSyncTask(ConnectionProvider connectionProvider,
                                     long orderId,
                                     CreationArgs.GenieAction action) {
        String
                sql = """                
                        UPDATE task
                        SET state = ?
                        where 
                        (input_json #>> '{creationArgs, orderId}')::int = ? and
                         input_json #>> '{creationArgs,requestedAction}' = ?
                """;

        try (Connection conn = connectionProvider.provideConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, TaskState.READY.ordinal());
            stmt.setLong(2, orderId);
            stmt.setString(3, action.name());
            stmt.executeUpdate();
        } catch (Exception ex) {
            log.error("Failed to retry fee mappings", ex);
        }
    }

    public List<TaskDto> getUpdate(ConnectionProvider connectionProvider, String qualifier) {
        String query = """
                    SELECT 
                        t.state, 
                        t.topic, 
                        t.sequence, 
                        COALESCE(
                            NULLIF(t.input::jsonb -> 'creationArgs' -> 'taskInput' ->> 'docExId', ''),
                            NULLIF(t.input::jsonb -> 'creationArgs' ->> 'docExId', '')
                        ) AS doc_ex_id
                    FROM task t
                    WHERE t.sequence IN (
                        SELECT subpath(tt.levels, nlevel(tt.levels)-1, 1)::text::bigint
                        FROM task_tree tt
                        WHERE tt.qualifier IN (
                            SELECT tt2.qualifier
                            FROM task_tree tt2
                            WHERE nlevel(tt2.levels) = 1
                        )
                        AND tt.qualifier = ?
                        AND nlevel(tt.levels) > 1
                        AND subpath(tt.levels, nlevel(tt.levels)-1, 1) <> '__END__'
                    )
                    AND (t.state = 3 OR t.topic = 'UPDATE_ENCOMPASS')
                """;

        List<TaskDto> taskList = new ArrayList<>();

        try (Connection conn = connectionProvider.provideConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, qualifier);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TaskDto task = new TaskDto();
                    task.setSequence(rs.getLong("sequence"));
                    task.setState(rs.getInt("state"));
                    task.setTopic(rs.getString("topic"));

                    String docExIdStr = rs.getString("doc_ex_id");
                    if (docExIdStr != null && !docExIdStr.isEmpty()) {
                        task.setDocExId(Integer.parseInt(docExIdStr));
                    }

                    taskList.add(task);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to fetch UPDATE_ENCOMPASS tasks by qualifier: {}", qualifier, ex);
        }

        return taskList;
    }


}
