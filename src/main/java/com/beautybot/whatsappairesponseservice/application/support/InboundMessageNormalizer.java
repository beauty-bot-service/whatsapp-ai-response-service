package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class InboundMessageNormalizer {

    private static final String DEFAULT_CHANNEL = "API";

    public ChatMessage normalize(ChatMessage request) {
        if (request == null) {
            return null;
        }
        return ChatMessage.builder()
                .phoneNumber(normalizePhoneNumber(request.getPhoneNumber()))
                .message(normalizeMessage(request.getMessage()))
                .channel(normalizeChannel(request.getChannel()))
                .externalMessageId(normalizeExternalMessageId(request.getExternalMessageId()))
                .build();
    }

    private String normalizePhoneNumber(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        return digits;
    }

    private String normalizeMessage(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizeChannel(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_CHANNEL;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeExternalMessageId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
