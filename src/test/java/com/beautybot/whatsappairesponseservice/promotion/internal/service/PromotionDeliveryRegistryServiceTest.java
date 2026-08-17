package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionDeliveryEntity;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionDeliveryRegistryServiceTest {

    @Mock
    private PromotionDeliveryRepository repository;

    @Test
    void filtersPromotionsDeliveredInCurrentSession() {
        PromotionDeliveryRegistryService service = new PromotionDeliveryRegistryService(repository);
        PromotionContent botox = new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX");
        PromotionContent rinomodelado = new PromotionContent(2L, "rino", "Rino", "PROMO RINO");
        when(repository.findDeliveredPromotionIds(10L)).thenReturn(List.of(1L));

        List<PromotionContent> result = service.filterUndelivered(10L, List.of(botox, rinomodelado));

        assertThat(result).containsExactly(rinomodelado);
    }

    @Test
    void recordsOnlyPromotionsIncludedInReply() {
        PromotionDeliveryRegistryService service = new PromotionDeliveryRegistryService(repository);
        PromotionContent promotion = new PromotionContent(2L, "rino", "Rino", "PROMO RINO");

        service.recordDelivered(10L, List.of(promotion));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PromotionDeliveryEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(delivery -> {
            assertThat(delivery.getConversationSessionId()).isEqualTo(10L);
            assertThat(delivery.getPromotionId()).isEqualTo(2L);
            assertThat(delivery.getPromotionCode()).isEqualTo("rino");
            assertThat(delivery.getDeliveredAt()).isNotNull();
        });
    }
}
