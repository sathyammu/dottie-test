package com.brimmatech.docflow.v2.task.listeners;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.processor.TaskListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j @AllArgsConstructor @Service public class TaskLoggingListener implements TaskListener<String> {


    private String combineTasksForLogging(Set<Task> tasks) {
        return tasks.stream().map(v -> Long.toString(v.getSequence())).collect(Collectors.joining(","));
    }

    @Override public void onFatal(String topic, Throwable throwable) {
        log.info("Recieved fatal exception on Topic:  {}", topic, throwable);
    }

    @Override public void onEmpty(String topic) {
        log.debug("No new tasks for topic: {}", topic);
    }

    @Override public String onStart(String topic, Set<Task> tasks) {
        log.info("Starting batch of tasks for topic: \n\t\t{}, {}", topic, combineTasksForLogging(tasks));
        return "";
    }

    @Override public void onDispatched(String topic, String trace, Set<Task> tasks) {
        log.info("Dispatched batch of tasks for topic: \n\t\t{}, {}", topic, combineTasksForLogging(tasks));
    }

    @Override public void onCallback(String topic, String trace, Set<Task> tasks) {
        log.info("Callback of tasks for topic: \n\t\t{}, {}", topic, combineTasksForLogging(tasks));
    }

    @Override public void onComplete(String topic, String trace, Map<Task, TaskDecision> decisions) {
    }

    @Override public void onError(String topic, String trace, Set<Task> tasks, Throwable throwable) {}

}
