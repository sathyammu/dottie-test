package com.brimmatech.docflow.v2.task.globalapi;

public sealed interface SubmittableJobOutcome<R>  {

    record Completed<R>(R result) implements SubmittableJobOutcome<R> {
        public Status getStatus() {
            return Status.Completed;
        }
    }
    record Suspended<R>(String reason, JobStatus checkpoint) implements SubmittableJobOutcome<R> {
        public Status getStatus() {
            return Status.Suspended;
        }
    }
    record Failed<R>(Throwable error) implements SubmittableJobOutcome<R> {
        public Status getStatus() {
            return Status.Failed;
        }
    }

    record JobStatus(
            String businessKey,
            String operationLocation ) {}

    Status getStatus();
    enum Status {
        Completed,
        Suspended,
        Failed
    }
}

