package com.beautybot.whatsappairesponseservice.promotion.internal.repository;

import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionEventRepository extends JpaRepository<PromotionEventEntity, Long> {
}
