package com.brimmatech.docflow.assistants;

import com.azure.ai.openai.assistants.AssistantsClient;
import com.azure.ai.openai.assistants.models.*;
import com.brimmatech.docflow.v2.services.settings_dto.OverlayAssistants;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class CustomTenantAssistantTool {

    private final Supplier<AssistantsClient> assistantFactory;
    private final OverlayAssistants assistantMeta;


    public CustomTenantAssistantTool(Supplier<AssistantsClient> assistantFactory,
                                     OverlayAssistants assistantMeta) {
        this.assistantFactory = assistantFactory;
        this.assistantMeta = assistantMeta;
    }

    public Function<ToolRegistryService.AssistantToolInput, String> getAssistantToolExecutor() {
        return userQuery -> {
            try {
                //TODO: Remove this duplicate code viz com.brimmatech.mcp.FannieMaeSellerCaller.getSellerGuidelines
                AssistantsClient client = assistantFactory.get();

                log.info("Running tenant-specific assistant: assistantName={}. LLm Query: {}",
                        assistantMeta.toolName(),
                        userQuery);

                val thread = client.createThread(new AssistantThreadCreationOptions().setMessages(List.of()));

                String threadId = thread.getId();
                client.createMessage(threadId,
                        new ThreadMessageOptions(MessageRole.USER, userQuery.queryForCallingAssistant()));


                ThreadRun run = client.createRun(threadId, new CreateRunOptions(assistantMeta.assistantId()));

                waitForRunCompletion(run, client);

                String assistantReply = extractAssistantReply(client, threadId);
                client.deleteThread(threadId);
                return assistantReply;
            } catch (Exception e) {
                log.error("Got exception : ", e);
                return "Unable to serve the data right now , please try again later";
            }

        };
    }


    private void waitForRunCompletion(ThreadRun run, AssistantsClient client) throws InterruptedException {
        while (run.getStatus() == RunStatus.QUEUED || run.getStatus() == RunStatus.IN_PROGRESS) {
            log.debug("Waiting for run {}: status={}", run.getId(), run.getStatus());
            Thread.sleep(1000);
            run = client.getRun(run.getThreadId(), run.getId());
        }
        log.info("Run {} completed with status {}", run.getId(), run.getStatus());
    }

    private String extractAssistantReply(AssistantsClient client, String threadId) {
        PageableList<ThreadMessage> messages = client.listMessages(threadId);

        return messages.getData().stream()
                .filter(msg -> msg.getRole() == MessageRole.ASSISTANT)
                .flatMap(msg -> msg.getContent().stream())
                .filter(MessageTextContent.class::isInstance)
                .map(content -> ((MessageTextContent) content).getText().getValue())
                .collect(Collectors.joining("\n"));
    }
}
