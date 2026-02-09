package com.brimmatech.docflow.v2.models.converters;

import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TaskBusinessFlowNameConverter
        implements Converter<String, TenantSettingsMeta.TaskBusinessFlowName> {

    @Override
    public TenantSettingsMeta.TaskBusinessFlowName convert(@NotNull String source) {
        return TenantSettingsMeta.TaskBusinessFlowName.fromValue(source);
    }
}