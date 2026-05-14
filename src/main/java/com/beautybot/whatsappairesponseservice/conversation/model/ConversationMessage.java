package com.beautybot.whatsappairesponseservice.conversation.model;

import com.beautybot.whatsappairesponseservice.conversation.state.MessageDirection;
import com.beautybot.whatsappairesponseservice.conversation.state.SenderType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationMessage {

    private Long id;
    private Long sessionId;
    private String phoneNumber;
    private MessageDirection direction;
    private SenderType senderType;
    private String channel;
    private String externalMessageId;
    private String content;
    private LocalDateTime createdAt;
}


