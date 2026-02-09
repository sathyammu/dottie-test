package com.brimmatech.docflow.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import static com.brimmatech.docflow.exception.DocflowDataException.DocFlowErrorCodes.INTERNAL_ERROR;

@Getter
@Setter
@Slf4j
public class DocflowDataException extends RuntimeException{
    private String message;
    private DocFlowErrorCodes code;
    public DocflowDataException(String message){
        super();
        this.message = message;
        this.code = INTERNAL_ERROR;
    }

    public DocflowDataException(String message, DocFlowErrorCodes code, Logger logger){
        super();
        log.error(message);
        this.message = message;
        this.code = code;
    }

    public DocflowDataException(String message, Exception e){
        super(e);
        this.message = message;
    }

    @RequiredArgsConstructor
    @Getter
    public enum DocFlowErrorCodes {
        INTERNAL_ERROR,
        API_ERROR;
    }
}
