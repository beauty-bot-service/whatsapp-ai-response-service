package com.beautybot.whatsappairesponseservice.persistence.entity;

import com.beautybot.whatsappairesponseservice.outbound.state.OutboundMessageStatus;
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
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "OUTBOUND_MESSAGES",
        indexes = {
                @Index(name = "IDX_OUTBOUND_MESSAGES_STATUS_CREATED_AT", columnList = "STATUS,CREATED_AT"),
                @Index(name = "IDX_OUTBOUND_MESSAGES_SESSION", columnList = "SESSION_ID"),
                @Index(name = "IDX_OUTBOUND_MESSAGES_PHONE", columnList = "PHONE_NUMBER"),
                @Index(name = "IDX_OUTBOUND_MESSAGES_EXTERNAL_MESSAGE_ID", columnList = "EXTERNAL_MESSAGE_ID")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SESSION_ID", nullable = false)
    private Long sessionId;

    @Column(name = "PHONE_NUMBER", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "CHANNEL", nullable = false, length = 32)
    private String channel;

    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "EXTERNAL_MESSAGE_ID", length = 120)
    private String externalMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 16)
    private OutboundMessageStatus status;

    @Column(name = "ATTEMPT_COUNT", nullable = false)
    private int attemptCount;

    @Column(name = "LAST_ERROR", length = 1000)
    private String lastError;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;
}
