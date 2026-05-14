package com.beautybot.whatsappairesponseservice.presentation.dto;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponse {

    private String reply;
    private ConversationState state;
    private Boolean requiresHuman;
}


