package com.beautybot.whatsappairesponseservice.application.exception;

public class InvalidChatMessageException extends RuntimeException {

    public InvalidChatMessageException(String message) {
        super(message);
    }
}
