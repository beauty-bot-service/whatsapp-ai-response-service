package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.PromotionDeliveryRegistry;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionDeliveryEntity;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromotionDeliveryRegistryService implements PromotionDeliveryRegistry {

    private final PromotionDeliveryRepository repository;

    @Override
    public List<PromotionContent> filterUndelivered(
            Long conversationSessionId,
            List<PromotionContent> candidates
    ) {
        if (conversationSessionId == null || candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Set<Long> deliveredIds = new HashSet<>(repository.findDeliveredPromotionIds(conversationSessionId));
        return candidates.stream()
                .filter(candidate -> candidate != null && candidate.id() != null)
                .filter(candidate -> !deliveredIds.contains(candidate.id()))
                .toList();
    }

    @Override
    public void recordDelivered(Long conversationSessionId, List<PromotionContent> deliveredPromotions) {
        if (conversationSessionId == null || deliveredPromotions == null || deliveredPromotions.isEmpty()) {
            return;
        }
        Instant deliveredAt = Instant.now();
        List<PromotionDeliveryEntity> deliveries = deliveredPromotions.stream()
                .map(promotion -> PromotionDeliveryEntity.builder()
                        .conversationSessionId(conversationSessionId)
                        .promotionId(promotion.id())
                        .promotionCode(promotion.code())
                        .deliveredAt(deliveredAt)
                        .build())
                .toList();
        repository.saveAll(deliveries);
    }
}
