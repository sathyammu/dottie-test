package com.brimmatech.mcp;

import com.brimmatech.docflow.v2.models.SpringAiChatMemory;
import com.brimmatech.docflow.v2.repositories.SpringAiChatMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlanningResultsHolder {

    private final SpringAiChatMemoryRepository springAiChatMemoryRepository;
    private final Supplier<LocalDateTime> nowUtcLocalDateTime;
    Map<String, String> planStore = new HashMap<>();

    @Tool(name = "getCurrentPlan", description = "Get the current plan for the current Session")
    public String getSellerGuidelines(String chatSession) {
        return planStore.getOrDefault(chatSession, "No plan found");
    }

    @Tool(name = "saveCurrentPlan", description = "Set the current plan for the current Session")
    public void getSellerGuidelines(String chatSession,
                                    @ToolParam(description = "A Markdown based todo listing that acts as a plan for " +
                                            "you to execute the current user query")
                                    String plan) {

        log.trace("Saving plan: \n\t{}", plan);
        SpringAiChatMemory memory = new SpringAiChatMemory();
        memory.setContent(plan);
        memory.setConversationId(chatSession);
        memory.setType("TOOL");
        memory.setTimestamp(nowUtcLocalDateTime.get());
        try {
            springAiChatMemoryRepository.save(memory);
        } catch(Exception e) {
            log.trace("Got exception while saving tool call", e);
        }

        planStore.put(chatSession, plan);
    }

    public void clearPlan(String session) {
        planStore.remove(session);
    }
}
