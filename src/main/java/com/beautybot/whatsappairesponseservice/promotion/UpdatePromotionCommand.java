package com.beautybot.whatsappairesponseservice.promotion;

import java.time.Instant;
import java.util.Set;

public record UpdatePromotionCommand(
        Long version,
        String code,
        String title,
        String messageBody,
        Set<String> aliases,
        Instant validFrom,
        Instant validUntil
) {
}
