package com.beautybot.whatsappairesponseservice.promotion.internal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Promotion not found.")
public class PromotionNotFoundException extends RuntimeException {

    public PromotionNotFoundException() {
        super("Promotion not found.");
    }
}
