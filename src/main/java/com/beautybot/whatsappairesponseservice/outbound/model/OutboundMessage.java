package com.beautybot.whatsappairesponseservice.outbound.model;

import com.beautybot.whatsappairesponseservice.outbound.state.OutboundMessageStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OutboundMessage {
    private Long id;
    private Long sessionId;
    private String phoneNumber;
    private String channel;
    private String content;
    private String externalMessageId;
    private OutboundMessageStatus status;
    private int attemptCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
}
