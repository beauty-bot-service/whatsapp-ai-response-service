package com.beautybot.whatsappairesponseservice.promotion;

import java.time.Instant;
import java.util.List;

public record PromotionView(
        Long id,
        Long clinicId,
        String code,
        String title,
        String messageBody,
        List<String> aliases,
        PromotionStatus status,
        Instant validFrom,
        Instant validUntil,
        boolean currentlyActive,
        Long version,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public PromotionView {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }
}
