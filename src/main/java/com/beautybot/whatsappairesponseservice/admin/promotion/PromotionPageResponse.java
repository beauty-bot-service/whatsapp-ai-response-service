package com.beautybot.whatsappairesponseservice.admin.promotion;

import com.beautybot.whatsappairesponseservice.promotion.PromotionView;
import org.springframework.data.domain.Page;

import java.util.List;

public record PromotionPageResponse(
        List<PromotionView> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {

    public static PromotionPageResponse from(Page<PromotionView> page) {
        return new PromotionPageResponse(
                List.copyOf(page.getContent()),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
