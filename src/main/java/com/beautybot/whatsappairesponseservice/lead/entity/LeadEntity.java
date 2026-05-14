package com.beautybot.whatsappairesponseservice.lead.entity;

import com.beautybot.whatsappairesponseservice.lead.model.LeadSource;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "LeadManagementLead")
@Table(
        name = "LEADS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_LEAD_CLINIC_PHONE", columnNames = {"CLINIC_ID", "PHONE_NUMBER"})
        },
        indexes = {
                @Index(name = "IDX_LEADS_CLINIC_STATUS", columnList = "CLINIC_ID,STATUS"),
                @Index(name = "IDX_LEADS_CLINIC_TEMPERATURE", columnList = "CLINIC_ID,TEMPERATURE"),
                @Index(name = "IDX_LEADS_PHONE", columnList = "PHONE_NUMBER"),
                @Index(name = "IDX_LEADS_CONVERSATION_SESSION", columnList = "CONVERSATION_SESSION_ID"),
                @Index(name = "IDX_LEADS_TREATMENT_INTEREST", columnList = "CLINIC_ID,TREATMENT_INTEREST"),
                @Index(name = "IDX_LEADS_CREATED_AT", columnList = "CREATED_AT")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CLINIC_ID", nullable = false)
    private Long clinicId;

    @Column(name = "CONVERSATION_SESSION_ID")
    private Long conversationSessionId;

    @Column(name = "PHONE_NUMBER", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "CUSTOMER_NAME", length = 150)
    private String customerName;

    @Column(name = "TREATMENT_INTEREST", length = 150)
    private String treatmentInterest;

    @Column(name = "FIRST_TIME")
    private Boolean firstTime;

    @Column(name = "PREFERRED_TIME", length = 255)
    private String preferredTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE", nullable = false, length = 50)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 50)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "TEMPERATURE", nullable = false, length = 20)
    private LeadTemperature temperature;

    @Column(name = "SCORE", nullable = false)
    private Integer score;

    @Column(name = "ASSIGNED_TO_USER_ID")
    private Long assignedToUserId;

    @Column(name = "LAST_CUSTOMER_MESSAGE_AT")
    private LocalDateTime lastCustomerMessageAt;

    @Column(name = "LAST_BOT_MESSAGE_AT")
    private LocalDateTime lastBotMessageAt;

    @Column(name = "LAST_HUMAN_MESSAGE_AT")
    private LocalDateTime lastHumanMessageAt;

    @Column(name = "READY_FOR_HUMAN_AT")
    private LocalDateTime readyForHumanAt;

    @Column(name = "APPOINTMENT_REQUESTED_AT")
    private LocalDateTime appointmentRequestedAt;

    @Column(name = "APPOINTMENT_BOOKED_AT")
    private LocalDateTime appointmentBookedAt;

    @Column(name = "LOST_AT")
    private LocalDateTime lostAt;

    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    @Column(name = "NOTES", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "METADATA", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = LeadStatus.NEW;
        }
        if (temperature == null) {
            temperature = LeadTemperature.COLD;
        }
        if (source == null) {
            source = LeadSource.UNKNOWN;
        }
        if (score == null) {
            score = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
