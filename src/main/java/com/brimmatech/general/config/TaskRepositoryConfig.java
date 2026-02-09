package com.brimmatech.general.config;

import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.task.listeners.SupervisorWaker;
import com.brimmatech.docflow.v2.task.repository.ConnectionProviderTaskRepository;
import com.brimmatech.general.infra.ConnectionProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.jdbc.PostgresTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Configuration
@Slf4j
public class TaskRepositoryConfig {

    @Autowired
    SupervisorWaker supervisorWaker;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ConnectionProvider connectionProvider;

    @Autowired
    TenantSettingsService tenantSettingsService;

    @Bean
    ConnectionProviderTaskRepository extractDocTypeTaskSink() throws SQLException {
        val postgresRepository = new PostgresTaskRepository(true, "foo");
        final var repo = new ConnectionProviderTaskRepository(postgresRepository, supervisorWaker,  objectMapper,tenantSettingsService);
        for (TemplateConfig.TOPICS topic : TemplateConfig.TOPICS.values()) {
            val isInited = repo.initialize(connectionProvider, topic.toString());
            log.trace("Inited?:{}", isInited);
        }
        repo.register(connectionProvider);
        return repo;
    }

}
