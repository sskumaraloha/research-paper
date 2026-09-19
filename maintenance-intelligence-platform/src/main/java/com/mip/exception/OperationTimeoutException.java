package com.mip.exception;

import org.springframework.http.HttpStatus;

/** An operation exceeded its server-side time budget and was aborted. */
public class OperationTimeoutException extends ApiException {

    public OperationTimeoutException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
