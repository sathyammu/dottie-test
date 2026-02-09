package com.brimmatech.docflow.v2.task.dto;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.extraction.DexStrategy;
import com.brimmatech.docflow.extraction.PollStrategy;
import com.brimmatech.docflow.v2.models.TaskTree;
import com.brimmatech.encompass.attachments.DownloadStrategy;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.With;
import lombok.val;

import java.util.*;
import java.util.function.Supplier;

public class CreationArgs {

    public enum GenericFieldUpdateAction {
        Validate,
        ValidateAndUpdate,
        Update
    }

    public enum GenieAction {
        FeeUpdate,
        ReWriteFeeToUi,
        OrderCreation
    }

    @Builder public record SleepTaskInput(@With int numSplits, @With int maxWaitMs) {

        public static SleepTaskInput defaults() {
            return SleepTaskInput.builder().numSplits(1).maxWaitMs(1000).build();
        }

    }

    public record ExtractionModels(String modelId, @With boolean isComplete,@With String operationLocation) {
    }

    /**
     * @param docExId       Applicable for any topic that takes in a model Id and name
     * @param modelType     Applicable for any topic that takes in a model Id and name
     * @param strategy      Only for EXTRACT_DOC_TYPE; differentiates DOC_INTEL and content_understanding strategies
     * @param pollStrategy  Only for EXTRACT_DOC_TYPE; differentiates REST_API and SDK     *
     * @param modelIds      Only for content_understanding strategy:  list of models to extract data from
     */
    @Builder public record TaskInputFromDex(@With int docExId,
                                            @With int changeLedgerId,
                                            String modelType,
                                            DexStrategy strategy,
                                            PollStrategy pollStrategy,
                                            Integer pageFrom,
                                            Integer pageTo,
                                            SettingsCategory promptStrategy,
                                            @With boolean isRetried,
                                            @With List<ExtractionModels> modelIds,
                                            @With int paDocExId,
                                            @With long paUpdatedMetaId,Map<String,String> traceHeaders,
                                            @With @JsonProperty("suspensionSpec")
                                            List<SuspensionSpecConfig.Entry> suspensionSpec,
                                            @With  Map<String, List<Object>> notes) {

        public TaskInputFromDex {
            suspensionSpec = (suspensionSpec == null) ? List.of() : List.copyOf(suspensionSpec);

        }

    }

    @Builder public record TaskInputFromChangeLedger(@With int changeLedgerId, String modelType, Map<String,String> traceHeaders, @With
    DownloadStrategy downloadStrategy) {
    }

    @Builder public record EncompassStepInput(@With boolean isCompleted,
                                              EncompassUpdateStrategy stepName,
                                              JsonNode stepArgs,
                                              GenericFieldUpdateArgs genericFieldUpdateArgs,
                                              @With String errorMessage,
                                              Map<String,String> traceHeaders) {
        public <T> T deserializeStepArgs(ObjectMapper mapper) {
            val deserializerClass = stepName.getType();

            if(Objects.isNull(deserializerClass)) {
                return (T) stepArgs;
            }else {
                return (T) ThrowingSupplier.getCapturingExceptions(() ->  mapper.treeToValue(stepArgs, deserializerClass)).orElse(null);
            }
        }
    }

    @Builder public record EncompassUpdateTaskInput(@With List<EncompassStepInput> steps,
                                                    @With TaskInputFromDex taskInput,
                                                    Map<String,String> traceHeaders,
                                                    @With List<EncompassUpdateDtos.EncompassUploadStatuses> uploadStatus) {

        public EncompassUpdateTaskInput {
            uploadStatus = (uploadStatus == null) ? new ArrayList<>() : new ArrayList<>(uploadStatus);
        }

    }

    @Builder public record HttpTaskInput(@With String requestFactory, @With int numSplits, @With JsonNode requestArgs) {
        public static HttpTaskInput defaults() {
            return HttpTaskInput.builder().numSplits(1).build();
        }
    }

    @Builder public record SyncWithGenieTaskInput(@With long orderId, @With GenieAction requestedAction) {
        public static SyncWithGenieTaskInput defaults() {
            return SyncWithGenieTaskInput.builder()
                    .requestedAction(GenieAction.FeeUpdate)
                    .build();
        }
    }

    public record SupervisorTerminationInput(int stepNumber, String topic) {
        public int nextStepNumber() {
            return stepNumber + 1;
        }
    }

    @Builder public record SupervisorTaskInput(String id, SupervisorTerminationInput termination, String check,Map<String,String> traceHeaders) {

        public boolean hasCheckEnabled() {
            return Optional.ofNullable(check).map(v -> v.isBlank() ? null : v).isPresent();
        }
    }

    @Builder public record GroupPagesTaskInput(
            int docExId,
            boolean shouldMockSplits,
            Map<String,String> traceHeaders,
            String level
    ) {
    }

    @Builder public record SplitTaskInput(int processingResultModelId,
                                          int docIndex,
                                          boolean processAllSplits,
                                          int docExId,
                                          boolean isRerun, Map<String,String> traceHeaders) {
    }

    @Builder public record ChildTaskInput(@With String identifier,
                                          @With JsonNode creationArgs,
                                          @With int priority,
                                          @With TOPICS topic,
                                          @With boolean isSuspended,
                                          @With boolean isInProgress,
                                          @With SupervisorTaskInput checkingSupervisor,
                                          @With String notificationTopic) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record SuspensionSpecConfig(
            @JsonProperty("suspensionSpec") List<Entry> entries
    ) {
        public SuspensionSpecConfig {
            entries = (entries == null) ? List.of() : List.copyOf(entries);
        }

        public List<Entry> getEntries() {
            return entries;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static record Entry(
                @JsonProperty("delaySpec") String delaySpec,
                @JsonProperty("successAction") String successAction,
                @JsonProperty("failureAction") String failureAction
        ) {
            public String getDelaySpec() {
                return delaySpec;
            }

            public String getSuccessAction() {
                return successAction;
            }

            public String getFailureAction() {
                return failureAction;
            }
        }
    }


    @Builder public record RootTaskInput(String identifier,
                                         @With JsonNode creationArgs,
                                         @With int priority,
                                         TOPICS topic,
                                         long tenantId,
                                         @With TaskTree parent,
                                         @With String runId,
                                         @With String notificationTopic, int batchSize, String processEnv, @With @JsonProperty("suspensionSpec")
                                         List<SuspensionSpecConfig.Entry> suspensionSpec) {
    }

    @Builder public record NotifyArgs(List<Integer> sequences, String notificationTopic) {
    }

    @Builder public record TaskInputArgs(String qualifier,
                                         @With String parent,
                                         long tenantId,
                                         @With int priority,
                                         @With String runId,
                                         @With JsonNode creationArgs,
                                         SupervisorTaskInput checkingSupervisor,
                                         @With String notificationTopic,
                                         @With String notes) {

        public static String rootTaskInput(RootTaskInput input, ObjectMapper objectMapper) {
            val
                    newArgs =
                    TaskInputArgs.builder()
                            .creationArgs(input.creationArgs())
                            .tenantId(input.tenantId())
                            .parent(input.parent().getLevels())
                            .runId(input.runId())
                            .priority(input.priority())
                            .tenantId(input.tenantId())
                            .notificationTopic(input.notificationTopic)
                            .qualifier(input.parent().getQualifier())
                            .build();
            try {
                return objectMapper.writeValueAsString(newArgs);
            } catch (Exception e) {
                return "{}";
            }
        }

        public String childTaskInput(ChildTaskInput input, ObjectMapper objectMapper) {
            val
                    newArgs =
                    TaskInputArgs.builder()
                            .parent(parent)
                            .runId(runId)
                            .priority(input.priority)
                            .qualifier(qualifier)
                            .tenantId(tenantId)
                            .creationArgs(input.creationArgs)
                            .checkingSupervisor(input.checkingSupervisor)
                            .notificationTopic(input.notificationTopic)
                            .build();
            try {
                return objectMapper.writeValueAsString(newArgs);
            } catch (Exception e) {
                return "{}";
            }
        }

        public <T> T getTypedInput(Class<T> type, ObjectMapper objectMapper, Supplier<T> defaultValue) {
            return ThrowingSupplier.getCapturingExceptions(() -> {
                return objectMapper.treeToValue(creationArgs, type);
            }).orElse(defaultValue.get());
        }
    }

    @Builder
    public record GenericFieldUpdateArgs(GenericFieldUpdateAction action) {
    }

}
