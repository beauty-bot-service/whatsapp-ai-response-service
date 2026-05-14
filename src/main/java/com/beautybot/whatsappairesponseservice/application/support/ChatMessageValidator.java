package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.application.exception.InvalidChatMessageException;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageValidator {

    private static final int MIN_PHONE_DIGITS = 8;
    private static final int MAX_PHONE_DIGITS = 20;
    private static final int MAX_MESSAGE_LENGTH = 4_000;
    private static final int MAX_CHANNEL_LENGTH = 32;
    private static final int MAX_EXTERNAL_MESSAGE_ID_LENGTH = 120;

    public void validateNormalized(ChatMessage message) {
        if (message == null) {
            throw new InvalidChatMessageException("mensaje invalido");
        }
        validatePhoneNumber(message.getPhoneNumber());
        validateMessage(message.getMessage());
        validateLength("channel", message.getChannel(), MAX_CHANNEL_LENGTH, false);
        validateLength("externalMessageId", message.getExternalMessageId(), MAX_EXTERNAL_MESSAGE_ID_LENGTH, true);
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            throw new InvalidChatMessageException("phoneNumber invalido");
        }
        if (!phoneNumber.matches("\\d+")) {
            throw new InvalidChatMessageException("phoneNumber debe contener solo digitos luego de normalizar");
        }
        if (phoneNumber.length() < MIN_PHONE_DIGITS || phoneNumber.length() > MAX_PHONE_DIGITS) {
            throw new InvalidChatMessageException("phoneNumber debe tener entre " + MIN_PHONE_DIGITS + " y " + MAX_PHONE_DIGITS + " digitos");
        }
    }

    private void validateMessage(String text) {
        if (isBlank(text)) {
            throw new InvalidChatMessageException("message invalido");
        }
        if (text.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidChatMessageException("message supera el maximo de " + MAX_MESSAGE_LENGTH + " caracteres");
        }
    }

    private void validateLength(String fieldName, String value, int maxLength, boolean nullable) {
        if (value == null) {
            if (!nullable) {
                throw new InvalidChatMessageException(fieldName + " invalido");
            }
            return;
        }
        if (value.length() > maxLength) {
            throw new InvalidChatMessageException(fieldName + " supera el maximo de " + maxLength + " caracteres");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
