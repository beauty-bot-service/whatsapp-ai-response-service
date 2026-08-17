package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.PromotionStatus;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEntity;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionCatalogServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-06T15:00:00Z");

    @Mock
    private PromotionRepository promotionRepository;

    private PromotionCatalogService service;

    @BeforeEach
    void setUp() {
        service = new PromotionCatalogService(
                promotionRepository,
                new PromotionMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void matchesMultiplePromotionsInMessageOrder() {
        PromotionEntity rinomodelado = promotion(1L, "rinomodelado", "Promo rino", "Rino", "nariz");
        PromotionEntity botox = promotion(2L, "botox", "Promo botox", "Botox", "toxina botulinica");
        when(promotionRepository.findByClinicIdAndStatusOrderByTitleAsc(1L, PromotionStatus.ACTIVE))
                .thenReturn(List.of(rinomodelado, botox));

        List<PromotionContent> matches = service.match(1L, "Quiero Botox y tambien lo de la nariz");

        assertThat(matches).extracting(PromotionContent::code)
                .containsExactly("botox", "rinomodelado");
    }

    @Test
    void ignoresExpiredAndFuturePromotions() {
        PromotionEntity expired = promotion(1L, "botox", "Promo botox", "Botox", "toxina");
        expired.setValidUntil(NOW);
        PromotionEntity future = promotion(2L, "rinomodelado", "Promo rino", "Rino", "nariz");
        future.setValidFrom(NOW.plusSeconds(60));
        when(promotionRepository.findByClinicIdAndStatusOrderByTitleAsc(1L, PromotionStatus.ACTIVE))
                .thenReturn(List.of(expired, future));

        assertThat(service.match(1L, "botox y nariz")).isEmpty();
        assertThat(service.findActive(1L)).isEmpty();
    }

    @Test
    void matchesSlashCodeAsNormalText() {
        PromotionEntity botox = promotion(1L, "botox", "Promo botox", "Contenido", "toxina");
        when(promotionRepository.findByClinicIdAndStatusOrderByTitleAsc(1L, PromotionStatus.ACTIVE))
                .thenReturn(List.of(botox));

        assertThat(service.match(1L, "/botox"))
                .extracting(PromotionContent::code)
                .containsExactly("botox");
    }

    private PromotionEntity promotion(Long id, String code, String title, String body, String... aliases) {
        return PromotionEntity.builder()
                .id(id)
                .clinicId(1L)
                .code(code)
                .title(title)
                .messageBody(body)
                .aliases(new LinkedHashSet<>(List.of(aliases)))
                .status(PromotionStatus.ACTIVE)
                .version(0L)
                .createdBy("admin@example.com")
                .updatedBy("admin@example.com")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
