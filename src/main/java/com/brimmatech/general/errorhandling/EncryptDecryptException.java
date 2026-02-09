package com.brimmatech.general.errorhandling;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EncryptDecryptException extends RuntimeException {

    private String message;
    private Exception exception;


    public EncryptDecryptException(String message) {
        this.message = message;
    }

    public EncryptDecryptException(String message, Exception exception) {
        this.message = message;
        this.exception = exception;
    }
}
