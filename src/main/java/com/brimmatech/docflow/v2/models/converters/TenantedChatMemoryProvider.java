package com.brimmatech.docflow.v2.models.converters;

import com.brimmatech.docflow.v2.services.settings_dto.A3_ChatSettings;
import com.brimmatech.saas.crytograph.EncryptDecryptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j @Component @RequiredArgsConstructor public class TenantedChatMemoryProvider {
    private final EncryptDecryptService cryptoService;
    private final JdbcChatMemoryRepository chatMemoryRepository;

    public ChatMemory via(A3_ChatSettings chatSettings) {
        return Optional.of(chatSettings)

                .flatMap(v -> {
                    if (!v.canSaveChatHistory()) {
                        return Optional.empty();
                    }
                    val
                            delegateChatMemory =
                            MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).build();

                    final EncryptedChatMemory
                            encryptedChatMemory =
                            new EncryptedChatMemory(delegateChatMemory, cryptoService);
                    encryptedChatMemory.setSaveMode(v.saveMode());
                    return Optional.of((ChatMemory) encryptedChatMemory);
                }).orElseGet(() -> {
                    InMemoryChatMemoryRepository repo = new InMemoryChatMemoryRepository();
                    return MessageWindowChatMemory.builder().chatMemoryRepository(repo).build();
                });
    }
}
