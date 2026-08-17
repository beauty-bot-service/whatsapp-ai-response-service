package com.beautybot.whatsappairesponseservice.promotion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromotionManagement {

    Page<PromotionView> search(Long clinicId, PromotionStatus status, String query, Pageable pageable);

    PromotionView getById(Long clinicId, Long promotionId);

    PromotionView create(Long clinicId, CreatePromotionCommand command, String actor);

    PromotionView update(Long clinicId, Long promotionId, UpdatePromotionCommand command, String actor);

    PromotionView activate(Long clinicId, Long promotionId, Long version, String actor);

    PromotionView archive(Long clinicId, Long promotionId, Long version, String actor);
}
