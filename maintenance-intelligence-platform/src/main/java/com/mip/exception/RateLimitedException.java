package com.mip.exception;

import org.springframework.http.HttpStatus;

public class RateLimitedException extends ApiException {

    public RateLimitedException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
