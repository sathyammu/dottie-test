package com.brimmatech.docflow.exception;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnAuthorizedException extends RuntimeException {
    private String message;

    public UnAuthorizedException(String message){
        super();
        this.message = message;
    }

    public UnAuthorizedException(String message, Exception e){
        super(e);
        this.message = message;
    }
}
