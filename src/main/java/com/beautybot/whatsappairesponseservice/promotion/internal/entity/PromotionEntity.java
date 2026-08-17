package com.beautybot.whatsappairesponseservice.promotion.internal.entity;

import com.beautybot.whatsappairesponseservice.promotion.PromotionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity(name = "PromotionModulePromotion")
@Table(
        name = "PROMOTIONS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_PROMOTIONS_CLINIC_CODE", columnNames = {"CLINIC_ID", "CODE"})
        },
        indexes = {
                @Index(name = "IDX_PROMOTIONS_CLINIC_STATUS", columnList = "CLINIC_ID,STATUS"),
                @Index(name = "IDX_PROMOTIONS_VALIDITY", columnList = "CLINIC_ID,VALID_FROM,VALID_UNTIL")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CLINIC_ID", nullable = false)
    private Long clinicId;

    @Column(name = "CODE", nullable = false, length = 50)
    private String code;

    @Column(name = "TITLE", nullable = false, length = 120)
    private String title;

    @Column(name = "MESSAGE_BODY", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "PROMOTION_ALIASES",
            joinColumns = @JoinColumn(name = "PROMOTION_ID")
    )
    @Column(name = "ALIAS", nullable = false, length = 80)
    private Set<String> aliases = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private PromotionStatus status;

    @Column(name = "VALID_FROM")
    private Instant validFrom;

    @Column(name = "VALID_UNTIL")
    private Instant validUntil;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @Column(name = "CREATED_BY", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "UPDATED_BY", nullable = false, length = 120)
    private String updatedBy;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (status == null) {
            status = PromotionStatus.DRAFT;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
