package com.brimmatech.docflow.classification.assistants;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service @Slf4j @RequiredArgsConstructor public class SystemPromptProvider {

    private final TenantSettingsService tenantSettingsService;
    @Value("classpath:prompts/princeton/summary-prompt.md") private Resource loanSummaryPrompt;

    public SystemMessage getSystemMessage(long tenantId, SettingsCategory sysPromptsFeeder) {
        if (!sysPromptsFeeder.isSystemPrompt()) {
            return null;
        }
        return tenantSettingsService.getSystemPromptWithGlobalDefaults(tenantId, sysPromptsFeeder)
                .map((v) -> SystemMessage.builder().text(v).build())
                .orElseGet(() -> {
                    Resource prompt = switch (sysPromptsFeeder) {
                        case SYS_PROMPTS_PRINCETON_SUMMARY -> loanSummaryPrompt;
                        default -> null;
                    };

                    if (prompt == null) {
                        return null;
                    }
                    return SystemMessage.builder().text(prompt).build();
                });
    }



    @Builder
    public record TemplatedPromptMeta<T>(
            String templateResourceName,
            String promptTemplateText,
            T templateContext
    ) {

    }

}