package com.beautybot.whatsappairesponseservice.conversation.model;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResult {

    private String reply;
    private ConversationState state;
    private Boolean requiresHuman;
    private ConversationSession session;
}



