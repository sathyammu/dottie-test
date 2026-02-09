package com.brimmatech.docflow.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocFlowDataProcessingException extends RuntimeException{

    private String message;

    private int value;

    public DocFlowDataProcessingException(String message) {
        super();
        this.message = message;
    }

    public DocFlowDataProcessingException(String message, Exception e){
        super(e);
        this.message = message;
    }

    public DocFlowDataProcessingException(String message, int value) {
        this.message = message;
        this.value = value;
    }

    public DocFlowDataProcessingException(String s, int value, Exception exception) {
        super(exception);
        this.message = message;
        this.value = value;
    }
}
