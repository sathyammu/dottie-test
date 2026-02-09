package com.brimmatech.docflow.v2.task;

import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.task.listeners.TaskLoggingListener;
import com.brimmatech.docflow.v2.task.repository.ConnectionProviderTaskRepository;
import com.brimmatech.docflow.v2.task.runners.*;
import com.brimmatech.general.infra.ConnectionProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.processor.BufferingTaskHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.concurrent.Executor;

@Component
@Scope("prototype")
@Getter
@Slf4j
@Profile("!test")
public class RunnerInjections {

    @Autowired
    ConnectionProviderTaskRepository extractDocTypeTaskSink;

    @Autowired
    ConnectionProvider connectionProvider;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    BufferingTaskHandlerFactory<ConnectionProvider, SQLException, Void> taskHandlerFactory;

    @Autowired
    TaskLoggingListener taskLoggingListener;

    @Autowired
    TenantSettingsService tenantSettingsService;

    @Autowired
    @Qualifier("task-runner-pool")
    Executor sequentialExecutor;
    @Value("${docflow.taskConfig.limiterBounds}")
    private int aiTasksLimiterBounds;
    @Value("${docflow.taskConfig.noOfWorkers}")
    private int noOfWorkers;

    @EventListener(ApplicationReadyEvent.class)
    public void tryStartTasks() throws InterruptedException {
        extractDocTypeTaskSink.setZombiesAsReady(connectionProvider);

        log.info("Trying to start runners");


        SupervisorRunner supervisorRunner = new SupervisorRunner(
                this);
        supervisorRunner.start();

        UpdateEncompassRunner encompassRunner = new UpdateEncompassRunner(this);
        encompassRunner.start();

        log.info("Started all runners");


    }

}
