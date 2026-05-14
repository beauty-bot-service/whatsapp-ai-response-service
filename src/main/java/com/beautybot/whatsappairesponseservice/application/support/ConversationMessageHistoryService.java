package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationMessageHistoryService {

    private final ConversationMessageService conversationMessageService;

    public boolean isInboundAlreadyProcessed(ChatMessage message) {
        return conversationMessageService.isInboundAlreadyProcessed(
                message.getChannel(),
                message.getExternalMessageId()
        );
    }

    public boolean saveInbound(ConversationSession session, ChatMessage message) {
        return conversationMessageService.saveInbound(
                session,
                message.getMessage(),
                message.getChannel(),
                message.getExternalMessageId()
        );
    }

    public void saveOutbound(ConversationSession session, String reply) {
        conversationMessageService.saveOutbound(session, reply);
    }

    public void saveHumanOutbound(ConversationSession session, String reply) {
        conversationMessageService.saveHumanOutbound(session, reply);
    }
}
