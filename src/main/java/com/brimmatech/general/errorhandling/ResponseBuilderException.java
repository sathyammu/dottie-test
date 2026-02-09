package com.brimmatech.general.errorhandling;

public class ResponseBuilderException extends RuntimeException{

    public ResponseBuilderException(String message, Exception exception){
        super(message, exception);
    }
}
