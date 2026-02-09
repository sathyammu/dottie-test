package com.brimmatech.docflow.v2.task.runners;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.docflow.v2.task.RunnerInjections;
import com.brimmatech.general.config.TemplateConfig;
import com.brimmatech.general.infra.ConnectionProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskSource.Condition;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskSource.Order;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.processor.TaskDispatcher;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.processor.TaskLimiter;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.processor.TaskManager;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.brimmatech.docflow.v2.services.TenantSettingsService.DEFAULT_TASK_PROCESSOR_CONFIG;

@Getter
@Slf4j
public abstract class BaseRunner {

    RunnerInjections injections;

    public BaseRunner(RunnerInjections injections) {
        this.injections = injections;

    }

    private Executor getThreadPool() {
        return this.injections.getSequentialExecutor();
    }

    protected TaskManager<ConnectionProvider, SQLException, String> sequentialTaskHandler(TemplateConfig.TOPICS topic,
                                                                                          int taskBufferSize) {

        val limiterBound = List.of(
                        TemplateConfig.TOPICS.UPDATE_ENCOMPASS)
                .contains(topic) ? 5 : 50;

        TenantSettingsService.TaskProcessorConfig
                taskProcessorConfig =
                injections.getTenantSettingsService()
                        .getGlobalSettingTyped(SettingsCategory.TASK_PROCESSOR_CONFIG,
                                TenantSettingsService.TaskProcessorConfig.class)
                        .orElse(DEFAULT_TASK_PROCESSOR_CONFIG);
        return new TaskManager<>(
                topic.toString(),
                getThreadPool(),
                injections.getExtractDocTypeTaskSink(),
                dispatcher(),
                TaskLimiter.bound(limiterBound),
                injections.getTaskHandlerFactory(),
                this.getInjections().getTaskLoggingListener(),
                Order.FIRST_IN_FIRST_OUT,
                Condition.SINGULAR_BY_IDENTIFIER,
                taskProcessorConfig.pullSize(), //Poll (Unit) task collection
                5,
                TimeUnit.SECONDS);
    }

    private TaskDispatcher<ConnectionProvider, SQLException> dispatcher() {
        return new TaskDispatcher<>() {

            @Override
            public <PAYLOAD> PAYLOAD apply(TransactionFunction<ConnectionProvider, SQLException, PAYLOAD> function)
                    throws SQLException {
                return function.apply(injections.getConnectionProvider());
            }
        };
    }


}
