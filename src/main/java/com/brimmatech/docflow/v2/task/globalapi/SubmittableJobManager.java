package com.brimmatech.docflow.v2.task.globalapi;


import com.brimmatech.docflow.v2.task.delegates.TaskDelegate;
import lombok.RequiredArgsConstructor;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmittableJobManager {
    
    private final SubmittableJobStatusRepository jobStatusRepository;

    public <R> SubmittableJobOutcome<R> run(
            TaskDelegate<?, ?> delegate,
            Task task,
            String businessKey,
            SubmittableJob<R> job
    ) {
        var operationCheckpoint = jobStatusRepository.load(delegate, task, businessKey)
                .orElse(new SubmittableJobOutcome.JobStatus(businessKey, null));
        try {
            String opLoc = operationCheckpoint.operationLocation();
            if (opLoc == null || opLoc.isBlank()) {
                opLoc = job.start(businessKey);
                operationCheckpoint = new SubmittableJobOutcome.JobStatus(businessKey, opLoc);
                jobStatusRepository.save(delegate, task, operationCheckpoint);
            }

            Optional<R> maybe = job.getStatus(opLoc);
            if (maybe.isEmpty()) {
                return new SubmittableJobOutcome.Suspended<>("NOT_READY", operationCheckpoint);
            }

            jobStatusRepository.delete(delegate, task, businessKey);
            return new SubmittableJobOutcome.Completed<>(maybe.get());

        } catch (Exception ex) {
            if (job.isTerminalNoResult(ex)) {
                jobStatusRepository.delete(delegate, task, businessKey);
            } else if (operationCheckpoint.operationLocation() != null) {
                jobStatusRepository.save(delegate, task, operationCheckpoint);
            }
            return new SubmittableJobOutcome.Failed<>(ex);
        }
    }
}
