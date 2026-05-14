package com.beautybot.whatsappairesponseservice.conversation.decision;

import com.beautybot.whatsappairesponseservice.conversation.state.MessageDirection;
import com.beautybot.whatsappairesponseservice.conversation.state.SenderType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentConversationMessage {
    private MessageDirection direction;
    private SenderType senderType;
    private String content;
}
