package com.mip.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends ApiException {

    public InvalidStateTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
