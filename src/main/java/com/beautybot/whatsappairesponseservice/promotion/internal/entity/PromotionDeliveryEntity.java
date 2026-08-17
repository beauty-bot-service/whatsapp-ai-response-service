package com.beautybot.whatsappairesponseservice.promotion.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity(name = "PromotionModuleDelivery")
@Table(
        name = "CONVERSATION_PROMOTION_DELIVERIES",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PROMOTION_DELIVERIES_SESSION_PROMOTION",
                columnNames = {"CONVERSATION_SESSION_ID", "PROMOTION_ID"}
        ),
        indexes = {
                @Index(
                        name = "IDX_PROMOTION_DELIVERIES_SESSION",
                        columnList = "CONVERSATION_SESSION_ID,DELIVERED_AT"
                ),
                @Index(name = "IDX_PROMOTION_DELIVERIES_PROMOTION", columnList = "PROMOTION_ID,DELIVERED_AT")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CONVERSATION_SESSION_ID", nullable = false)
    private Long conversationSessionId;

    @Column(name = "PROMOTION_ID", nullable = false)
    private Long promotionId;

    @Column(name = "PROMOTION_CODE", nullable = false, length = 50)
    private String promotionCode;

    @Column(name = "DELIVERED_AT", nullable = false)
    private Instant deliveredAt;
}
