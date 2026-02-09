package com.brimmatech.docflow.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanLockException extends RuntimeException{

    private String message;

    public LoanLockException(String message){
        super();
        this.message = message;
    }

    public LoanLockException(String message, Exception e){
        super(e);
        this.message = message;
    }
}
