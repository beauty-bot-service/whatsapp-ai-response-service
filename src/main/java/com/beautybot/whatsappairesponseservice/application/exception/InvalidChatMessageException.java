package com.beautybot.whatsappairesponseservice.application.exception;

public class InvalidChatMessageException extends AppException {

    public InvalidChatMessageException(ResponseCode responseCode, Object... messageArgs) {
        super(responseCode, messageArgs);
    }
}
