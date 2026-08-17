package com.beautybot.whatsappairesponseservice.promotion.internal.entity;

import com.beautybot.whatsappairesponseservice.promotion.internal.model.PromotionEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity(name = "PromotionModuleEvent")
@Table(
        name = "PROMOTION_EVENTS",
        indexes = {
                @Index(name = "IDX_PROMOTION_EVENTS_PROMOTION", columnList = "PROMOTION_ID,CREATED_AT"),
                @Index(name = "IDX_PROMOTION_EVENTS_CLINIC", columnList = "CLINIC_ID,CREATED_AT")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROMOTION_ID", nullable = false)
    private Long promotionId;

    @Column(name = "CLINIC_ID", nullable = false)
    private Long clinicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 30)
    private PromotionEventType eventType;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "CREATED_BY", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
