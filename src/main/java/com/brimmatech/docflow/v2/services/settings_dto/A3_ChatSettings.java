package com.brimmatech.docflow.v2.services.settings_dto;

import com.brimmatech.docflow.v2.models.converters.EncryptedChatMemory;
import lombok.Builder;

@Builder
public record A3_ChatSettings(boolean canSaveChatHistory,
                              EncryptedChatMemory.SaveMode saveMode) {
}
