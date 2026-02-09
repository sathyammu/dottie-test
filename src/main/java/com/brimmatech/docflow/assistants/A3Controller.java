package com.brimmatech.docflow.assistants;

import com.brimmatech.docflow.classification.assistants.SystemPromptProvider;
import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.v2.models.converters.EncryptedChatMemory;
import com.brimmatech.docflow.v2.models.converters.TenantedChatMemoryProvider;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.services.settings_dto.A3_ChatSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static com.brimmatech.docflow.enums.SettingsCategory.SYS_PROMPTS_PRINCETON_SUMMARY;

@RequiredArgsConstructor @RestController @Slf4j @RequestMapping("/a3") public class A3Controller {

    private static final A3_ChatSettings DEFAULT_CHAT_SETTINGS = A3_ChatSettings.builder()
            .canSaveChatHistory(true)
            .saveMode(EncryptedChatMemory.SaveMode.ALL)
            .build();
    private final ChatClient.Builder llmClientBuilder;
    private final SystemPromptProvider systemPromptProvider;
    private final TenantedChatMemoryProvider tenantedChatMemoryProvider;
    private final ToolRegistryService toolRegistryService;
    private final TenantSettingsService tenantSettingsService;
    private ToolRegistryService.CacheEntry cachedChatSettings;

    @PostMapping("/summary/{tenantId}/{email}")
    public ResponseEntity<Flux<String>> getLoanSummaryResponse(@RequestBody ChatDto chatBody,
                                                               @PathVariable long tenantId,
                                                               @PathVariable String email) {

        ChatMemory memory = getChatMemory(getChatSettings(tenantId));
        cachedChatSettings = toolRegistryService.getOrCreateChatClientForTenant(tenantId, llmClientBuilder.clone(),
                getChatTopic(email));
        val client = cachedChatSettings.chatClient();
        Flux<String> flux;
        flux =
                client.prompt()
                        .system(systemPromptProvider.getSystemMessage(-1, SYS_PROMPTS_PRINCETON_SUMMARY).getText())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, getChatTopic(email)))
                        .advisors(MessageChatMemoryAdvisor.builder(memory).build())
                        .user(chatBody.loanObject.toString())
                        .stream()
                        .content();

        return ResponseEntity.status(HttpStatus.OK).body(flux);
    }

    private static @NotNull String getChatTopic(String email) {
        return String.format("a3-%s", email);
    }

    private A3_ChatSettings getChatSettings(long tenantId) {
        return tenantSettingsService.getSettingTyped(tenantId,
                SettingsCategory.A3_CHAT_SETTINGS,
                A3_ChatSettings.class,
                DEFAULT_CHAT_SETTINGS);
    }

    private ChatMemory getChatMemory(A3_ChatSettings chatSettings) {
        return tenantedChatMemoryProvider.via(chatSettings);
    }

    public record ChatDto(String message, Object loanObject) {
    }

}
