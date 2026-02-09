package com.brimmatech.general.config;


import com.azure.ai.openai.assistants.AssistantsClient;
import com.azure.ai.openai.assistants.AssistantsClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class AzureAssistantConfig {

    @Value("${openai.assistants_api.endpoint}") private String endpoint;
    @Value("${openai.assistants_api.api_key}") private String apiKey;

    @Bean Supplier<AssistantsClient> assistantFactory() {
        return () -> new AssistantsClientBuilder().credential(new AzureKeyCredential(apiKey))
                .endpoint(endpoint)
                .buildClient();
    }

}
