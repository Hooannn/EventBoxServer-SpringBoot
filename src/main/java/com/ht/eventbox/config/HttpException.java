package com.ht.eventbox.config;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HttpException extends RuntimeException {
    private final HttpStatus code;

    public HttpException(String message, HttpStatus status) {
        super(message);
        this.code = status;
    }

    public HttpStatus getStatus() {
        return code;
    }
}
