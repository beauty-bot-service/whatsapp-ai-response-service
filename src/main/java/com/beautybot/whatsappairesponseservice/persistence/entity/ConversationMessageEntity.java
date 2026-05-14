package com.beautybot.whatsappairesponseservice.persistence.entity;

import com.beautybot.whatsappairesponseservice.conversation.state.MessageDirection;
import com.beautybot.whatsappairesponseservice.conversation.state.SenderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "CONVERSATION_MESSAGES",
        indexes = {
                @Index(name = "IDX_CONVERSATION_MESSAGES_SESSION_CREATED_AT", columnList = "SESSION_ID,CREATED_AT"),
                @Index(name = "IDX_CONVERSATION_MESSAGES_PHONE_CREATED_AT", columnList = "PHONE_NUMBER,CREATED_AT"),
                @Index(
                        name = "UQ_CONVERSATION_MESSAGES_CHANNEL_EXTERNAL_MESSAGE_ID",
                        columnList = "CHANNEL,EXTERNAL_MESSAGE_ID",
                        unique = true
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SESSION_ID", nullable = false)
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "SESSION_ID",
            referencedColumnName = "ID",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "FK_CONVERSATION_MESSAGES_SESSION")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ConversationSessionEntity session;

    @Column(name = "PHONE_NUMBER", nullable = false, length = 32)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIRECTION", nullable = false, length = 16)
    private MessageDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "SENDER_TYPE", nullable = false, length = 16)
    private SenderType senderType;

    @Column(name = "CHANNEL", length = 32)
    private String channel;

    @Column(name = "EXTERNAL_MESSAGE_ID", length = 120)
    private String externalMessageId;

    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}


