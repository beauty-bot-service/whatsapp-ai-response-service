package com.beautybot.whatsappairesponseservice.admin.promotion;

import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;

import java.util.List;

public record PromotionMatchResponse(List<PromotionContent> matches) {
    public PromotionMatchResponse {
        matches = matches == null ? List.of() : List.copyOf(matches);
    }
}
