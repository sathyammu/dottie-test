package com.brimmatech.docflow.assistants;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.azure.ai.openai.assistants.AssistantsClient;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.services.settings_dto.OverlayAssistants;
import com.brimmatech.mcp.FannieMaeSellerCaller;
import com.brimmatech.mcp.LoanContextProvider;
import com.brimmatech.mcp.PlanningResultsHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.brimmatech.docflow.enums.SettingsCategory.A3_ASSISTANT_OVERLAYS;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolRegistryService {

    private final TenantSettingsService tenantSettingsService;
    private final Supplier<AssistantsClient> assistantFactory;
    private final LoanContextProvider loanContextProvider;
    private final FannieMaeSellerCaller fannieMaeSellerCaller;
    private final PlanningResultsHolder planningResultsHolder;

    private final Map<Long, CacheEntry> tenantToolCache = new ConcurrentHashMap<>();

    public CacheEntry getOrCreateChatClientForTenant(long tenantId, ChatClient.Builder llmClientBuilder,
                                                     @NotNull String chatTopic) {

        return tenantToolCache.computeIfAbsent(tenantId,
                (_tenantId) -> buildToolsForTenant(_tenantId, llmClientBuilder, chatTopic));
    }


    private CacheEntry buildToolsForTenant(long tenantId, ChatClient.Builder llmClientBuilder,
                                           @NotNull String chatTopic) {
        List<ToolCallback> tools = new ArrayList<>();
        Collections.addAll(tools,
                ToolCallbacks.from(planningResultsHolder, loanContextProvider, fannieMaeSellerCaller));
        final List<ToolCallback> resolvedTools = new ArrayList<>();

        tenantSettingsService.getSettingTyped(tenantId,
                        A3_ASSISTANT_OVERLAYS,
                        new TypeReference<List<OverlayAssistants>>() {
                        })

                .ifPresent(configs -> {

                    configs.forEach(_meta -> {
                        _meta.isAllPresent().ifPresent(meta -> {
                            log.info("Registering tenant custom tool for tenant {}: {}", tenantId, meta.assistantId());
                            CustomTenantAssistantTool tenantTool =
                                    new CustomTenantAssistantTool(assistantFactory,
                                            meta
                                    );

                            ToolCallback toolCallback = FunctionToolCallback
                                    .builder(meta.toolName(), tenantTool.getAssistantToolExecutor())
                                    .description(meta.toolDescription())
                                    .inputType(AssistantToolInput.class)
                                    .build();

                            Collections.addAll(tools, toolCallback);
                            resolvedTools.addAll(tools.reversed());
                        });
                    });
                });

        return CacheEntry.builder().chatClient(llmClientBuilder.defaultToolCallbacks(tools).build())
                .tools(resolvedTools.toArray(ToolCallback[]::new))
                .session(NanoIdUtils.randomNanoId()).chatTopic(chatTopic).build();
    }

    public record AssistantToolInput(String queryForCallingAssistant) {
    }

    @Builder
    public record CacheEntry(ToolCallback[] tools, ChatClient chatClient, String session, String chatTopic) {
    }
}


