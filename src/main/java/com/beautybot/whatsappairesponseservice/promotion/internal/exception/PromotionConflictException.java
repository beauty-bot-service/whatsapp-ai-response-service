package com.beautybot.whatsappairesponseservice.promotion.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PromotionConflictException extends RuntimeException {

    public PromotionConflictException(String message) {
        super(message);
    }

    public PromotionConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
