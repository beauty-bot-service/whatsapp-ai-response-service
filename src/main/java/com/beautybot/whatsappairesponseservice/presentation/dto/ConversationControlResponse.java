package com.beautybot.whatsappairesponseservice.presentation.dto;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationControlResponse {
    private Long sessionId;
    private String phoneNumber;
    private ConversationState state;
    private Boolean requiresHuman;
    private LocalDateTime updatedAt;
}
