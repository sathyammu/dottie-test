package com.brimmatech.docflow.v2.task.globalapi;

import java.util.Optional;

public interface SubmittableJob<R> {

    String start(String businessKey) throws Exception;

    Optional<R> getStatus(String operationLocation) throws Exception;

    default boolean isTerminalNoResult(Throwable ex) { return false; }
}
