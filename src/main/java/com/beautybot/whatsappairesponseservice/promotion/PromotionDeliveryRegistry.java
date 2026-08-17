package com.beautybot.whatsappairesponseservice.promotion;

import java.util.List;

public interface PromotionDeliveryRegistry {

    List<PromotionContent> filterUndelivered(Long conversationSessionId, List<PromotionContent> candidates);

    void recordDelivered(Long conversationSessionId, List<PromotionContent> deliveredPromotions);
}
