package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import java.text.Normalizer;
import java.util.Locale;

final class PromotionTextNormalizer {

    private PromotionTextNormalizer() {
    }

    static String normalizePhrase(String value) {
        if (value == null) {
            return "";
        }
        String lower = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return lower.replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    static String normalizeCode(String value) {
        String normalized = normalizePhrase(value == null ? null : value.replaceFirst("^/+", ""));
        return normalized.replace(' ', '-');
    }
}
