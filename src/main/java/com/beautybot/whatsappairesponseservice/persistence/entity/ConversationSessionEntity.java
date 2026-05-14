package com.beautybot.whatsappairesponseservice.persistence.entity;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.ContactPreference;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "CONVERSATION_SESSIONS",
        indexes = {
                @Index(name = "IDX_CONVERSATION_SESSIONS_PHONE", columnList = "PHONE_NUMBER"),
                @Index(name = "IDX_CONVERSATION_SESSIONS_PHONE_UPDATED_AT", columnList = "PHONE_NUMBER,UPDATED_AT"),
                @Index(name = "IDX_CONVERSATION_SESSIONS_STATE_UPDATED_AT", columnList = "STATE,UPDATED_AT")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PHONE_NUMBER", nullable = false, length = 32)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATE", nullable = false, length = 32)
    private ConversationState state;

    @Column(name = "CUSTOMER_NAME", length = 120)
    private String customerName;

    @Column(name = "TREATMENT_INTEREST", length = 120)
    private String treatmentInterest;

    @Column(name = "FIRST_TIME")
    private Boolean firstTime;

    @Column(name = "PREFERRED_TIME", length = 120)
    private String preferredTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "CONTACT_PREFERENCE", length = 32)
    private ContactPreference contactPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "WAITING_FOR_FIELD", length = 32)
    private RequiredField waitingForField;

    @Column(name = "REQUIRES_HUMAN", nullable = false)
    private Boolean requiresHuman;

    @Column(name = "SUMMARY_FOR_HUMAN", columnDefinition = "TEXT")
    private String summaryForHuman;

    @Column(name = "HUMAN_NOTIFIED_AT")
    private LocalDateTime humanNotifiedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}


