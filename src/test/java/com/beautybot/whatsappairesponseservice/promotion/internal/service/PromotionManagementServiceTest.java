package com.beautybot.whatsappairesponseservice.promotion.internal.service;

import com.beautybot.whatsappairesponseservice.promotion.CreatePromotionCommand;
import com.beautybot.whatsappairesponseservice.promotion.PromotionStatus;
import com.beautybot.whatsappairesponseservice.promotion.PromotionView;
import com.beautybot.whatsappairesponseservice.promotion.internal.entity.PromotionEntity;
import com.beautybot.whatsappairesponseservice.promotion.internal.exception.PromotionConflictException;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionEventRepository;
import com.beautybot.whatsappairesponseservice.promotion.internal.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-06T15:00:00Z");

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private PromotionEventRepository promotionEventRepository;

    private PromotionManagementService service;

    @BeforeEach
    void setUp() {
        service = new PromotionManagementService(
                promotionRepository,
                promotionEventRepository,
                new PromotionMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsDraftWithNormalizedCodeAndAliases() {
        when(promotionRepository.existsByClinicIdAndCode(1L, "botox-verano")).thenReturn(false);
        when(promotionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PromotionEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            entity.setVersion(0L);
            return entity;
        });

        PromotionView created = service.create(
                1L,
                new CreatePromotionCommand(
                        "/Botox Verano",
                        "Botox verano",
                        "Precio promocional de verano.",
                        Set.of("Toxina", "toxína"),
                        null,
                        null
                ),
                "admin@example.com"
        );

        assertThat(created.code()).isEqualTo("botox-verano");
        assertThat(created.status()).isEqualTo(PromotionStatus.DRAFT);
        assertThat(created.aliases()).hasSize(1);
        assertThat(PromotionTextNormalizer.normalizePhrase(created.aliases().getFirst())).isEqualTo("toxina");
        verify(promotionEventRepository).save(any());
    }

    @Test
    void rejectsDuplicatedCode() {
        when(promotionRepository.existsByClinicIdAndCode(1L, "botox")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                1L,
                new CreatePromotionCommand("botox", "Botox", "Contenido", Set.of(), null, null),
                "admin@example.com"
        )).isInstanceOf(PromotionConflictException.class);
    }
}
