package com.beautybot.whatsappairesponseservice.promotion.internal.repository;

import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromotionDeliveryRepository extends JpaRepository<PromotionDeliveryEntity, Long> {

    @Query("""
            select delivery.promotionId
            from PromotionModuleDelivery delivery
            where delivery.conversationSessionId = :conversationSessionId
            """)
    List<Long> findDeliveredPromotionIds(@Param("conversationSessionId") Long conversationSessionId);
}
