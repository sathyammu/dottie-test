package com.brimmatech.docflow.exception;

import com.azure.core.exception.HttpResponseException;
import com.brimmatech.docflow.enums.TaskFailures;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import static com.brimmatech.general.config.WebClientConfiguration.RETRIABLE_STATUSUES;

@Getter
@Setter
public class TaskFailureException extends Exception {
    private String code;
    private String message;
    private HttpStatus statusCode;

    public TaskFailureException(String code, String message) {
        super();
        this.message = message;
        this.code = code;
    }

    public TaskFailureException(TaskFailures code, String message) {
        super();
        this.message = message;
        this.code = code.getName();
    }

    public TaskFailureException(TaskFailures code) {
        super();
        this.message = code.getName();
        this.code = code.getName();
    }

    public TaskFailureException(String code, String message, Exception e) {
        super(e);
        this.code = code;
        this.message = message;
    }

    public TaskFailureException(HttpResponseException exception) {
        super(exception);
        statusCode = HttpStatus.valueOf(exception.getResponse().getStatusCode());
        this.code = code;
        this.message = message;
    }

    public boolean canBeRetried() {
        return RETRIABLE_STATUSUES.contains(this.statusCode);
    }
}
