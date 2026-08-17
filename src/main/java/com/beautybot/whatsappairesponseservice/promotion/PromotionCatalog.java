package com.beautybot.whatsappairesponseservice.promotion;

import java.util.List;

public interface PromotionCatalog {

    List<PromotionSummary> findActive(Long clinicId);

    List<PromotionContent> match(Long clinicId, String message);

    List<PromotionContent> findActiveByCodes(Long clinicId, List<String> codes);
}
