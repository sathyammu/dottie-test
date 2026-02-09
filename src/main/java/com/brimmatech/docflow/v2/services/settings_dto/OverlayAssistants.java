package com.brimmatech.docflow.v2.services.settings_dto;

import com.brimmatech.general.utils.OptionUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public record OverlayAssistants(
        //TODO: right now we support only assistant tool calls.
        String toolName,
        String toolDescription,
        String assistantId,
        String threadId,
        List<Capabilities> capabilities
) {
    public Optional<OverlayAssistants> isAllPresent() {
        return Optional.of(List.of(toolName, toolDescription, assistantId, threadId)).flatMap(l -> {
            //TODO: validate assistantId and threadId by checking using the Azure API for presence.
            // Else inform Bravo Support / Add service health
            return OptionUtils.fromBoolean(l.stream().noneMatch(String::isEmpty));
        }).map(v -> {
            return this;
        });
    }

    public enum Capabilities {
        TOC,
    }
}

