package com.beautybot.whatsappairesponseservice.whatsapp;

import org.springframework.stereotype.Component;

@Component
public class WhatsAppRecipientPhoneNormalizer {

    private static final String ARGENTINA_MOBILE_PREFIX = "549";
    private static final int ARGENTINA_WHATSAPP_ID_LENGTH = 13;

    public String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String normalized = phoneNumber.trim();
        if (normalized.length() == ARGENTINA_WHATSAPP_ID_LENGTH
                && normalized.startsWith(ARGENTINA_MOBILE_PREFIX)
                && normalized.chars().allMatch(Character::isDigit)) {
            return "54" + normalized.substring(ARGENTINA_MOBILE_PREFIX.length());
        }
        return normalized;
    }
}
