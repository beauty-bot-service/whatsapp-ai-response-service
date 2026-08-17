package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.PromotionSummary;
import com.beautybot.whatsappairesponseservice.promotion.PromotionView;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
class PromotionMapper {

    PromotionSummary toSummary(PromotionEntity entity) {
        return new PromotionSummary(entity.getId(), entity.getCode(), entity.getTitle(), sortedAliases(entity));
    }

    PromotionContent toContent(PromotionEntity entity) {
        return new PromotionContent(entity.getId(), entity.getCode(), entity.getTitle(), entity.getMessageBody());
    }

    PromotionView toView(PromotionEntity entity, Instant now) {
        return new PromotionView(
                entity.getId(),
                entity.getClinicId(),
                entity.getCode(),
                entity.getTitle(),
                entity.getMessageBody(),
                sortedAliases(entity),
                entity.getStatus(),
                entity.getValidFrom(),
                entity.getValidUntil(),
                PromotionCatalogService.isEffectiveAt(entity, now),
                entity.getVersion(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private List<String> sortedAliases(PromotionEntity entity) {
        if (entity.getAliases() == null) {
            return List.of();
        }
        return entity.getAliases().stream()
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }
}
