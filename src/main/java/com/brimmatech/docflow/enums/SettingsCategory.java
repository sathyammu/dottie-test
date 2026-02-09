package com.brimmatech.docflow.enums;

import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RequiredArgsConstructor
@Getter
public enum SettingsCategory {

    TASK_TASK_ROUTE("task", "task_route", new TypeReference<List<TenantSettingsMeta>>() {
    }),
    TASK_PROCESSOR_CONFIG("task", "processor_config", null),

    EMAIL_RX("email", "rx", null),

    AUTH_API_KEY("auth", "api_key", new TypeReference<String>() {
    }),

    ENCOMPASS_ALIAS("encompass", "alias", null),
    //A3
    A3_CHAT_SETTINGS("a3", "chat_settings", null),
    A3_ASSISTANT_OVERLAYS("a3", "assistant_overlays", null),
    SYS_PROMPTS_A3(Constants.SYS_PROMPTS, "a3", null),



    //Global
    SYS_PROMPTS_PRINCETON_SUMMARY(Constants.SYS_PROMPTS, "princeton-summary", null),

    //SYSTEM
    SYSTEM_SETTINGS_USE_CACHE(Constants.SYSTEM, "use_cache", null),
    SYSTEM_SUPPORT_TASK_COMPLETION_ALERTING(Constants.SYSTEM, "task_completion_alerting", null),

    SUSPENSION_STRATEGY("suspension", "suspension_strategy", null);

    public static final List<@NotNull SettingsCategory>
            ADDITIONAL_GLOBAL_PROMPTS =
            List.of(SYS_PROMPTS_PRINCETON_SUMMARY);

    private final String name;
    private final String strategy;
    private final TypeReference type;

    public String getCategory() {
        return name;
    }

    public boolean isGlobalPrompt() {
        return  ADDITIONAL_GLOBAL_PROMPTS.contains(this);

    }

    public boolean isSystemPrompt() {
        return this.name.equals(Constants.SYS_PROMPTS);
    }


    public static class Constants {
        public static final String SYS_PROMPTS = "sys_prompts";
        public static final String CLASSIFICATION = "classification";
        public static final String SYSTEM = "system";
    }


}
