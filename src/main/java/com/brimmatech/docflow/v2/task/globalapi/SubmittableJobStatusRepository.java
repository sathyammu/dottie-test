package com.brimmatech.docflow.v2.task.globalapi;

import com.brimmatech.docflow.v2.task.delegates.TaskDelegate;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;

import java.util.Optional;

public interface SubmittableJobStatusRepository {
    Optional<SubmittableJobOutcome.JobStatus> load(TaskDelegate<?, ?> delegate, Task task, String businessKey);
    void save(TaskDelegate<?, ?> delegate, Task task, SubmittableJobOutcome.JobStatus checkpoint);
    void delete(TaskDelegate<?, ?> delegate, Task task, String businessKey);
}
