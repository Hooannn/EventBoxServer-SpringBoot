package com.ht.eventbox.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String errorMessage = error.getDefaultMessage();
            errors.add(errorMessage);
        });
        return ResponseEntity.badRequest().body(
                ExceptionResponse
                        .builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message(errors.toString())
                        .build()
        );
    }

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<ExceptionResponse> handleHttpException(HttpException ex) {
        return ResponseEntity.status(ex.getStatus()).body(
                ExceptionResponse
                        .builder()
                        .code(ex.getStatus().value())
                        .message(ex.getMessage())
                        .build()
        );
    }
}
