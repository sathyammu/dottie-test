package com.brimmatech.docflow.v2.models.converters;

import com.brimmatech.saas.crytograph.EncryptDecryptService;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

public final class EncryptedChatMemory implements ChatMemory {
    private static final int DEFAULT_MAX_MESSAGES = 20;
    private  final ChatMemory delegateMemory;
    private  final  EncryptDecryptService cryptoService;
    @Setter
    private SaveMode saveMode = SaveMode.ALL;

    public EncryptedChatMemory(ChatMemory delegateMemory, EncryptDecryptService cryptoService) {
        this.delegateMemory = delegateMemory;
        this.cryptoService = cryptoService;
    }

    @Override
    public void add(@NotNull String conversationId, @NotNull Message message) {

        if (saveMode == SaveMode.ONLY_QUESTIONS && message.getMessageType() == MessageType.ASSISTANT) {
            return;
        }

        Message encryptedMessage = new EncryptedMessage(
                message.getMessageType(),
                cryptoService.encrypt(message.getText()),
                message.getMetadata()
        );
        delegateMemory.add(conversationId, encryptedMessage);
    }

    @Override
    public void add(@NotNull String conversationId, List<Message> messages) {
        List<Message> encryptedMessages = messages.stream()
                .filter(m -> !(saveMode == SaveMode.ONLY_QUESTIONS && m.getMessageType() == MessageType.ASSISTANT))
                .map(m -> new EncryptedMessage(
                        m.getMessageType(),
                        cryptoService.encrypt(m.getText()),
                        m.getMetadata()
                ))
                .collect(Collectors.toList());

        delegateMemory.add(conversationId, encryptedMessages);
    }

    @Override
    public @NotNull List<Message> get(@NotNull String conversationId) {
        return delegateMemory.get(conversationId).stream()
                .map(m -> {
                    String decryptedText = cryptoService.decryptAsString(m.getText());
                    if (m.getMessageType() == MessageType.USER) {
                        return new UserMessage(decryptedText);
                    } else if (m.getMessageType() == MessageType.ASSISTANT) {
                        return new AssistantMessage(decryptedText, m.getMetadata());
                    } else {
                        return m;
                    }
                })
                .limit(DEFAULT_MAX_MESSAGES)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(@NotNull String conversationId) {
        delegateMemory.clear(conversationId);
    }

    public enum SaveMode {
        ALL, ONLY_QUESTIONS
    }
}
