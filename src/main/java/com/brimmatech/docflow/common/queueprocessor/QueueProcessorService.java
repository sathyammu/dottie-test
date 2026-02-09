package com.brimmatech.docflow.common.queueprocessor;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.brimmatech.docflow.common.webhook.WebhookDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueueProcessorService {

    private final ObjectMapper objectMapper;

    public WebhookDto extractWebhookData(HttpServletRequest request) throws IOException {
        String webhookResponse = request.getReader().lines().collect(Collectors.joining("\n"));
        return objectMapper.readValue(webhookResponse, WebhookDto.class);
    }
}
