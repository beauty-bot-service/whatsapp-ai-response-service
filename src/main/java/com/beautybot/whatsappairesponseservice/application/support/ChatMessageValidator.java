package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.application.exception.InvalidChatMessageException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
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
            throw new InvalidChatMessageException(ResponseCode.INVALID_CHAT_MESSAGE);
        }
        validatePhoneNumber(message.getPhoneNumber());
        validateMessage(message.getMessage());
        validateLength("channel", message.getChannel(), MAX_CHANNEL_LENGTH, false);
        validateLength("externalMessageId", message.getExternalMessageId(), MAX_EXTERNAL_MESSAGE_ID_LENGTH, true);
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            throw new InvalidChatMessageException(ResponseCode.INVALID_PHONE_NUMBER);
        }
        if (!phoneNumber.matches("\\d+")) {
            throw new InvalidChatMessageException(ResponseCode.PHONE_NUMBER_MUST_BE_NUMERIC);
        }
        if (phoneNumber.length() < MIN_PHONE_DIGITS || phoneNumber.length() > MAX_PHONE_DIGITS) {
            throw new InvalidChatMessageException(ResponseCode.PHONE_NUMBER_LENGTH_OUT_OF_RANGE, MIN_PHONE_DIGITS, MAX_PHONE_DIGITS);
        }
    }

    private void validateMessage(String text) {
        if (isBlank(text)) {
            throw new InvalidChatMessageException(ResponseCode.INVALID_MESSAGE_TEXT);
        }
        if (text.length() > MAX_MESSAGE_LENGTH) {
            throw new InvalidChatMessageException(ResponseCode.MESSAGE_LENGTH_EXCEEDED, MAX_MESSAGE_LENGTH);
        }
    }

    private void validateLength(String fieldName, String value, int maxLength, boolean nullable) {
        if (value == null) {
            if (!nullable) {
                throw new InvalidChatMessageException(ResponseCode.INVALID_FIELD_VALUE, fieldName);
            }
            return;
        }
        if (value.length() > maxLength) {
            throw new InvalidChatMessageException(ResponseCode.FIELD_LENGTH_EXCEEDED, fieldName, maxLength);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
