package com.beautybot.whatsappairesponseservice.lead.entity;

import com.beautybot.whatsappairesponseservice.lead.model.LeadEventType;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
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

import java.time.LocalDateTime;

@Entity(name = "LeadManagementLeadEvent")
@Table(
        name = "LEAD_EVENTS",
        indexes = {
                @Index(name = "IDX_LEAD_EVENTS_LEAD_ID", columnList = "LEAD_ID"),
                @Index(name = "IDX_LEAD_EVENTS_CLINIC_CREATED_AT", columnList = "CLINIC_ID,CREATED_AT")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "LEAD_ID", nullable = false)
    private Long leadId;

    @Column(name = "CLINIC_ID", nullable = false)
    private Long clinicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 80)
    private LeadEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "PREVIOUS_STATUS", length = 50)
    private LeadStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "NEW_STATUS", length = 50)
    private LeadStatus newStatus;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
