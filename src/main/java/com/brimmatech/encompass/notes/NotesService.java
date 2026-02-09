package com.brimmatech.encompass.notes;

import com.brimmatech.docflow.common.webhook.WebhookDto;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.v2.dto.LoanUpdateDTO;
import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.brimmatech.docflow.v2.models.ChangeLedgerEntity;
import com.brimmatech.docflow.v2.repository.ChangeLedgerRepository;
import com.brimmatech.docflow.v2.task.TaskFactory;
import com.brimmatech.encompass.loanreader.EncompassLoanReader;
import com.brimmatech.encompass.loanreader.LoanReader;
import com.brimmatech.encompass.loanreader.fieldReader.FieldReaderResponse;
import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import com.brimmatech.encompass.loanupdater.LOSLoanUpdateService;
import com.brimmatech.general.errorhandling.ValliaDataException;
import com.brimmatech.mcp.LoanContextProvider;
import com.brimmatech.saas.PipelinePaginationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotesService {

    private final TenantEntityRepository tenantEntityRepository;
    private final LoanContextProvider loanContextProvider;
    private final ObjectMapper objectMapper;
    private final LOSLoanUpdateService losLoanUpdateService;
    private final TaskFactory taskFactory;
    private final LoanNotesRepository loanNotesRepository;
    private final EncompassLoanReader encompassLoanReader;
    private final ChangeLedgerRepository changeLedgerRepository;
    private final LoanReader loanReader;

    private static final int BATCH_SIZE = 25;
    private static final int THREADS = 5;

    private static final String BORR_NOTES = "CX.PROC.BORR.NOTES";
    private static final String CRIT_NOTES = "CX.PROC.CRITICAL.NOTES";
    private static final String STEP_UPDATE = "UPDATE_LOAN_NOTES";
    public static final String STRING_EMPTY = "";

    public JsonNode checkLoanLockAndUpdateEncompass(String loanId,
                                                    long tenantId,
                                                    List<NotesRequest> updates) {

        log.trace("NOTES_UPDATE_START | tenantId={} loanId={} updatesCount={}",
                tenantId, loanId, updates.size());

        TenantEntity tenant = getTenant(tenantId);
        String token = getToken(tenantId);

        NotesRequest history = findHistoryField(updates, loanId);
        ArrayNode notes = parseJsonArray(history.getValue());

        List<FieldReaderResponse> loanResponse =
                loanReader.fetchLoanDetails(loanId, List.of(CRIT_NOTES, BORR_NOTES), token);

        Map<String, String> fields = loanResponse.stream()
                .collect(Collectors.toMap(
                        FieldReaderResponse::getFieldId,
                        f -> Objects.toString(f.getValue(), "").trim(),
                        (a, b) -> a
                ));

        String existingNote = fields.getOrDefault(CRIT_NOTES,"");

        log.trace("NOTES_PARSED | loanId={} notesCount={}", loanId, notes.size());

        LoanNotes saved = saveNotesJson(tenant, loanId, notes);

        log.trace("NOTES_DB_SAVED | loanId={} tenantId={}", loanId, tenantId);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"));

        String formattedLatest = String.format("%s: %s : %s", timestamp, history.getUserInitial(), history.getLatestNote());

        String mergeNotes = mergeNotes(formattedLatest, existingNote);

        loanNotesTransfer(token, tenantId, loanId, buildLoanUpdates(mergeNotes));

        log.trace("NOTES_UPDATE_DONE | loanId={} tenantId={}", loanId, tenantId);

        return saved.getNotes();
    }

    public JsonNode getNotes(long tenantId, String loanGuid) {

        log.trace("GET_NOTES_START | tenantId={} loanGuid={}", tenantId, loanGuid);

        TenantEntity tenant = getTenant(tenantId);

        LoanNotes loanNotes = loanNotesRepository
                .findByTenantAndLoanGuid(tenant, loanGuid)
                .orElseThrow(() -> {
                    log.warn("GET_NOTES_NOT_FOUND | tenantId={} loanGuid={}", tenantId, loanGuid);
                    return new DocflowDataException("Notes not found");
                });

        ArrayNode notesArray = (ArrayNode) loanNotes.getNotes();

        for (JsonNode node : notesArray) {
            if (node.has("Comment")) {
                String comment = node.get("Comment").asText();

                String cleaned = cleanComment(comment);

                ((ObjectNode) node).put("Comment", cleaned);
            }
        }

        log.trace("GET_NOTES_DONE | tenantId={} loanGuid={}", tenantId, loanGuid);

        return notesArray;
    }


    public Map<String, Object> syncLegacyNotesBatch(long tenantId) {

        log.trace("BATCH_SYNC_START | tenantId={}", tenantId);

        TenantEntity tenant = getTenant(tenantId);
        String token = getToken(tenantId);

        PipelinePaginationRequest pipelineRequest = parsePipelineRequest(tenant.getPipelineRequest());
        List<PipelinePaginationResponse> loans = encompassLoanReader.fetchEligibleLoans(pipelineRequest, token);

        log.trace("BATCH_SYNC_LOANS_FETCHED | tenantId={} loans={}", tenantId, loans.size());

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        Map<String, String> results = new ConcurrentHashMap<>();

        try {

            List<List<PipelinePaginationResponse>> batches = chunk(loans, BATCH_SIZE);

            log.trace("BATCH_SYNC_BATCHES_CREATED | tenantId={} batchCount={} batchSize={}",
                    tenantId, batches.size(), BATCH_SIZE);

            for (int i = 0; i < batches.size(); i++) {

                List<PipelinePaginationResponse> batch = batches.get(i);

                log.trace("BATCH_SYNC_EXECUTING | batch={} size={}", i + 1, batch.size());

                List<CompletableFuture<Void>> jobs =
                        batch.stream()
                                .map(loan ->
                                        CompletableFuture.runAsync(() ->
                                                processSingleLoan(token, tenant, loan, results), executor))
                                .toList();

                CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new)).join();

                log.trace("BATCH_SYNC_COMPLETED | batch={}", i + 1);
            }

        } finally {
            executor.shutdown();
            log.trace("BATCH_SYNC_EXECUTOR_SHUTDOWN | tenantId={}", tenantId);
        }

        long success = results.values().stream().filter("MIGRATED"::equals).count();
        long noNotes = results.values().stream().filter("NO_LEGACY_NOTES"::equals).count();
        long failed  = results.values().stream().filter(v -> v.startsWith("FAILED")).count();

        log.trace("BATCH_SYNC_SUMMARY | tenantId={} success={} noNotes={} failed={}",
                tenantId, success, noNotes, failed);

        return Map.of(
                "tenantId", tenantId,
                "totalLoans", loans.size(),
                "successCount", success,
                "noNotesCount", noNotes,
                "failureCount", failed,
                "results", new TreeMap<>(results)
        );
    }

    private void processSingleLoan(String token,
                                   TenantEntity tenant,
                                   PipelinePaginationResponse loan,
                                   Map<String, String> results) {

        String loanGuid = loan.getLoanId();

        log.debug("MIGRATION_START | loanGuid={}", loanGuid);

        try {

            String legacyNotes = combineLegacyNotes(loan.getFields());

            if (legacyNotes.isBlank()) {
                log.trace("MIGRATION_SKIPPED_NO_NOTES | loanGuid={}", loanGuid);
                results.put(loanGuid, "NO_LEGACY_NOTES");
                return;
            }

            ArrayNode parsed = convertLegacyNotes(legacyNotes, "", "", "");
            saveNotesJson(tenant, loanGuid, parsed);

            loanNotesTransfer(token, tenant.getId(), loanGuid, buildLoanUpdates(parsed.asText()));

            log.trace("MIGRATION_SUCCESS | loanGuid={}", loanGuid);
            results.put(loanGuid, "MIGRATED");

        } catch (Exception e) {
            log.error("MIGRATION_FAILED | loanGuid={}", loanGuid, e);
            results.put(loanGuid, "FAILED - " + e.getMessage());
        }
    }

    private TenantEntity getTenant(long tenantId) {
        log.debug("RESOLVE_TENANT | tenantId={}", tenantId);
        return tenantEntityRepository.findById(tenantId)
                .orElseThrow(() -> {
                    log.error("TENANT_NOT_FOUND | tenantId={}", tenantId);
                    return new DocflowDataException("TenantId " + tenantId + " not registered");
                });
    }

    private String getToken(long tenantId) {
        log.debug("FETCH_TOKEN | tenantId={}", tenantId);
        return loanContextProvider.getToken(tenantId).getAccessToken();
    }

    private NotesRequest findHistoryField(List<NotesRequest> updates, String loanId) {

        log.debug("FIND_HISTORY_FIELD | loanId={} lookingFor={}", loanId, CRIT_NOTES);

        return updates.stream()
                .filter(u -> CRIT_NOTES.equalsIgnoreCase(u.getId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("HISTORY_FIELD_MISSING | loanId={}", loanId);
                    return new RuntimeException(CRIT_NOTES + " missing");
                });
    }

    private PipelinePaginationRequest parsePipelineRequest(String json) {

        log.debug("PARSE_PIPELINE_REQUEST");

        try {
            return objectMapper.readValue(json, PipelinePaginationRequest.class);
        } catch (Exception e) {
            log.error("PIPELINE_PARSE_FAILED");
            throw new RuntimeException("Invalid pipelineRequest JSON", e);
        }
    }

    private String combineLegacyNotes(Map<String, String> fields) {

        if (fields == null || fields.isEmpty()) return "";

        String proc = fields.getOrDefault("Fields." + CRIT_NOTES, "").trim();
        String borr = fields.getOrDefault("Fields." + BORR_NOTES, "").trim();

        return (!proc.isBlank() && !borr.isBlank()) ? proc + "\n" + borr
                : !proc.isBlank() ? proc
                : !borr.isBlank() ? borr
                : "";
    }

    private List<LoanUpdateDTO> buildLoanUpdates(String notes) {

        LoanUpdateDTO borr = new LoanUpdateDTO();
        borr.setId(CRIT_NOTES);
        borr.setValue(notes);

        LoanUpdateDTO clear = new LoanUpdateDTO();
        clear.setId(BORR_NOTES);
        clear.setValue("");

        return List.of(borr, clear);
    }

    private LoanNotes saveNotesJson(TenantEntity tenant,
                                    String loanGuid,
                                    JsonNode notes) {

        log.debug("SAVE_NOTES_DB | loanGuid={}", loanGuid);

        LoanNotes loanNotes = loanNotesRepository
                .findByTenantAndLoanGuid(tenant, loanGuid)
                .orElseGet(() -> {
                    log.debug("CREATE_NOTES_RECORD | loanGuid={}", loanGuid);
                    LoanNotes ln = new LoanNotes();
                    ln.setTenant(tenant);
                    ln.setLoanGuid(loanGuid);
                    return ln;
                });

        loanNotes.setNotes(notes);

        LoanNotes saved = loanNotesRepository.save(loanNotes);

        log.debug("SAVE_NOTES_DONE | loanGuid={}", loanGuid);

        return saved;
    }

    private ArrayNode parseJsonArray(String json) {

        log.debug("PARSE_JSON_ARRAY");

        try {
            JsonNode node = objectMapper.readTree(json);

            if (!node.isArray()) {
                log.error("INVALID_JSON_FORMAT | expected=array");
                throw new IllegalArgumentException("Expected JSON array");
            }

            return (ArrayNode) node;

        } catch (Exception e) {
            log.error("JSON_PARSE_FAILED");
            throw new RuntimeException("Invalid notes JSON", e);
        }
    }

    private ArrayNode convertLegacyNotes(String text,
                                         String user,
                                         String eventTime,
                                         String role) {

        log.debug("CONVERT_LEGACY_NOTES | user={} role={} eventTime={}", user, role, eventTime);

        ArrayNode array = objectMapper.createArrayNode();

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.US);

        ZonedDateTime parsedDate;

        try {
            if (StringUtils.hasText(eventTime)) {
                parsedDate = ZonedDateTime.parse(eventTime);
            } else {
                parsedDate = ZonedDateTime.now(ZoneOffset.UTC);
            }
        } catch (Exception ex) {
            log.warn("INVALID_EVENT_TIME | value={}, defaulting to UTC now", eventTime);
            parsedDate = ZonedDateTime.now(ZoneOffset.UTC);
        }

        String formattedDate = parsedDate.format(outputFormatter);

        ObjectNode note = objectMapper.createObjectNode();
        note.put("UserName", StringUtils.hasText(user) ? user : "SYSTEM_MIGRATION");
        note.put("Role", StringUtils.hasText(role) ? role : "SYSTEM");
        note.put("Date", formattedDate);
        note.put("Comment", text.trim());

        array.add(note);
        return array;
    }

    private String formatToHistoryText(ArrayNode notes) {

        StringBuilder sb = new StringBuilder();

        for (JsonNode note : notes) {

            sb.append("UserName - ").append(value(note, "UserName")).append("\n")
                    .append("Date     - ").append(value(note, "Date")).append("\n")
                    .append("Role     - ").append(value(note, "Role")).append("\n")
                    .append("Comment  - ").append(value(note, "Comment")).append("\n")
                    .append("---------------\n");
        }

        return sb.toString();
    }

    private String value(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : "";
    }

    private <T> List<List<T>> chunk(List<T> list, int size) {
        if (list == null || list.isEmpty()) return List.of();
        if (size <= 0) throw new IllegalArgumentException("Invalid batch size");

        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            batches.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        return batches;
    }

    public String loanNotesTransfer(String token,
                                    long tenantId,
                                    String loanId,
                                    List<LoanUpdateDTO> updates) {

        Map<String, List<LoanUpdateDTO>> request = Map.of(loanId, updates);

        log.trace("ENCOMPASS_UPDATE_START | loanId={}", loanId);

        try {
            losLoanUpdateService.updateLoanFields(request, token);
            log.trace("ENCOMPASS_UPDATE_SUCCESS | loanId={}", loanId);
            return "SUCCESS";

        } catch (WebClientResponseException e) {

            if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {

                log.warn("LOAN_LOCKED | loanId={} schedulingRetry", loanId);

                taskFactory.initiateJobForUpdateEncompass(
                        List.of(updates),
                        loanId,
                        tenantId,
                        STEP_UPDATE);

                log.trace("RETRY_SCHEDULED | loanId={}", loanId);

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        e.getResponseBodyAsString()
                );

            } else if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {

                log.error("ENCOMPASS_BAD_REQUEST | loanId={} response={}",
                        loanId, e.getResponseBodyAsString());
                throw new DocflowDataException(e.getResponseBodyAsString());

            } else {

                log.error("ENCOMPASS_ERROR | loanId={} status={} response={}",
                        loanId, e.getStatusCode(), e.getResponseBodyAsString());
                throw new ValliaDataException(e.getResponseBodyAsString());
            }
        }
    }

    public void updateNotesFromEncompass(long tenantId,
                                         TenantSettingsMeta.TaskBusinessFlowName flowName,
                                         WebhookDto webhookDto) {

        TenantEntity tenant = getTenant(tenantId);

        persistWebhookLedger(tenant, flowName, webhookDto);

        String token = getToken(tenant.getId());
        String loanId = webhookDto.getMeta().getResourceId();

        log.trace("WEBHOOK_RECEIVED | tenantId={} loanId={} user={}",
                tenantId, loanId, webhookDto.getMeta().getUserId());

        List<FieldReaderResponse> loanResponse =
                loanReader.fetchLoanDetails(loanId, List.of(CRIT_NOTES, BORR_NOTES), token);

        Map<String, String> fields = loanResponse.stream()
                .collect(Collectors.toMap(
                        FieldReaderResponse::getFieldId,
                        f -> Objects.toString(f.getValue(), "").trim(),
                        (a, b) -> a
                ));

        String incomingNote = fields.getOrDefault(BORR_NOTES, "");

        String existingNote = fields.getOrDefault(CRIT_NOTES,"");


        if (incomingNote.isBlank()) {
            log.trace("WEBHOOK_SKIPPED_EMPTY_NOTE | loanId={}", loanId);
            return;
        }

        ArrayNode notes = loanNotesRepository
                .findByTenantAndLoanGuid(tenant, loanId)
                .map(LoanNotes::getNotes)
                .filter(JsonNode::isArray)
                .map(n -> (ArrayNode) n)
                .orElseGet(() -> objectMapper.createArrayNode());

        ArrayNode newEntry = convertLegacyNotes(incomingNote, webhookDto.getMeta().getUserId(), webhookDto.getEventTime(), "");

        notes.addAll(newEntry);

        log.trace("NOTES_UPDATE_START | tenantId={} loanId={} updatesCount=1", tenantId, loanId);

        saveNotesJson(tenant, loanId, notes);

        log.trace("NOTES_DB_SAVED | loanId={} tenantId={}", loanId, tenantId);

        String mergeNotes = mergeNotes(incomingNote, existingNote);

        loanNotesTransfer(token, tenant.getId(), loanId, buildLoanUpdates(mergeNotes));

        log.trace("NOTES_UPDATE_DONE | loanId={} tenantId={}", loanId, tenantId);
    }

    private void persistWebhookLedger(TenantEntity tenant,
                                      TenantSettingsMeta.TaskBusinessFlowName flowName,
                                      WebhookDto dto) {

        try {
            ChangeLedgerEntity entity = new ChangeLedgerEntity();
            entity.setTenant(tenant);
            entity.setDocQualifier(objectMapper.valueToTree(dto));
            entity.setFlowName(flowName);

            changeLedgerRepository.saveAndFlush(entity);

        } catch (Exception e) {
            log.error("CHANGE_LEDGER_FAILED", e);
        }
    }

    private String mergeNotes(String incomingNote, String existingNote) {

        incomingNote = incomingNote == null ? STRING_EMPTY : incomingNote;
        existingNote = existingNote == null ? STRING_EMPTY : existingNote;

        if (incomingNote.isBlank() && existingNote.isBlank()) {
            return STRING_EMPTY;
        }

        if (existingNote.isBlank()) {
            return incomingNote;
        }

        if (incomingNote.isBlank()) {
            return existingNote;
        }
        return incomingNote + "\n\n" + existingNote;
    }

    private String cleanComment(String comment) {

        if (comment == null || comment.isBlank()) {
            return comment;
        }

        String trimmed = comment.trim();

        // 1. Do NOT modify email-style comments
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("from:") || lower.startsWith("sent:") || lower.startsWith("subject:")) {
            return comment;
        }

        // 2. Split multiple lines
        String[] lines = trimmed.split("\\r?\\n");
        StringBuilder result = new StringBuilder();

        // Pattern: DATE [optional @ time] [optional dash/colon] REST
        Pattern p = Pattern.compile(
                "^\\s*(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)" +
                        "(?:\\s*@?\\s*\\d{1,2}:\\d{2}(?:\\s*(AM|PM))?)?" +
                        "(?:\\s*[-:])?" +
                        "\\s*(.*)$"
        );

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher m = p.matcher(line);

            if (m.matches()) {
                // ALWAYS group 3 = full remaining text
                String remaining = m.group(3);
                result.append(remaining).append("\n");
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString().trim();
    }



}
