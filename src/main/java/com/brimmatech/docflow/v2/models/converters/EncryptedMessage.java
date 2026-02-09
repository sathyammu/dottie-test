package com.brimmatech.docflow.v2.models.converters;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.Map;

public record EncryptedMessage(
        MessageType messageType,
        String encryptedText,
        Map<String,Object> metadata
) implements Message {

    @Override
    public String getText() {
        return encryptedText; // This will be stored encrypted
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public MessageType getMessageType() {
        return messageType;
    }
}

