package com.brimmatech.mcp;

import com.azure.ai.openai.assistants.AssistantsClient;
import com.azure.ai.openai.assistants.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class FannieMaeSellerCaller {

    private final Supplier<AssistantsClient> assistantFactory;

    @Tool(name="fannieMaeSellingGuideHelper", description = "Get guidelines from FannieMae Seller guide")
    public String getSellerGuidelines(String userQuery) throws InterruptedException {
        try {
            AssistantsClient assistantsClient = assistantFactory.get();

            log.info("Running Fannie Mae assistant. LLm Query: {}", userQuery);

            String assistantId = "asst_PC7l7HEdQsncoN9C1CZf9Y7b";

            val thread = assistantsClient.createThread(new AssistantThreadCreationOptions().setMessages(List.of()));

            String threadId = thread.getId();
            assistantsClient.createMessage(threadId,
                    new ThreadMessageOptions(MessageRole.USER, userQuery));

            val threadAndRun = assistantsClient.createRun(threadId, new CreateRunOptions(assistantId));

            waitOnRun(threadAndRun, threadAndRun.getThreadId(), assistantsClient);
            val response =  getResponse(threadAndRun.getThreadId(), assistantsClient);
            assistantsClient.deleteThread(threadId);
            return response;
        } catch (Exception e) {
            log.error("Got exception : ", e);
            return "Unable to serve the data right now , please try again later";
        }


    }

    private  String getResponse(String threadId, AssistantsClient client) {
        PageableList<ThreadMessage> messages = client.listMessages(threadId);
        List<ThreadMessage> data = messages.getData();
        List<String> response = new ArrayList<>();
        for (ThreadMessage dataMessage : data) {
            MessageRole role = dataMessage.getRole();
            for (MessageContent messageContent : dataMessage.getContent()) {
                MessageTextContent messageTextContent = (MessageTextContent) messageContent;
                if (role.equals(MessageRole.ASSISTANT)) {
                    response.add(messageTextContent.getText().getValue());
                }
            }
        }
        return String.join("\n", response);
    }

    private  ThreadRun waitOnRun(ThreadRun run, String threadId, AssistantsClient client) throws InterruptedException {
        // Poll the Run in a loop
        while (run.getStatus() == RunStatus.QUEUED || run.getStatus() == RunStatus.IN_PROGRESS) {
            String runId = run.getId();
            run = client.getRun(threadId, runId);
            System.out.println("Run ID: " + runId + ", Run Status: " + run.getStatus());
            Thread.sleep(1000); // Sleep for 1 second before polling again
        }
        return run;
    }
}
