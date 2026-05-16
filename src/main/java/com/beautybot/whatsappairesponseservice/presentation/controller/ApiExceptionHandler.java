package com.beautybot.whatsappairesponseservice.presentation.controller;

import com.beautybot.whatsappairesponseservice.application.exception.AppException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, String>> handleAppException(AppException ex) {
        log.debug("Handled application exception. code={}, message={}", ex.getResponseCode().getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "responseCode", ex.getResponseCode().getCode(),
                        "error", ex.getUserMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Handled illegal argument exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "responseCode", ResponseCode.BAD_REQUEST.getCode(),
                        "error", ResponseCode.BAD_REQUEST.userMessage()
                ));
    }
}
