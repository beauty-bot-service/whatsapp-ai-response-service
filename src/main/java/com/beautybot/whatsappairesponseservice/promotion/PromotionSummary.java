package com.beautybot.whatsappairesponseservice.promotion;

import java.util.List;

public record PromotionSummary(
        Long id,
        String code,
        String title,
        List<String> aliases
) {
    public PromotionSummary {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }
}
