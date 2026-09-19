package com.mip.exception;

import org.springframework.http.HttpStatus;

public class FileProcessingException extends ApiException {

    public FileProcessingException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
