package com.beautybot.whatsappairesponseservice.application.decision.context;

import com.beautybot.whatsappairesponseservice.conversation.decision.RecentConversationMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationMessage;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationMessageService;
import com.beautybot.whatsappairesponseservice.conversation.state.MessageDirection;
import com.beautybot.whatsappairesponseservice.conversation.state.SenderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RecentMessageContextProvider {

    private static final int RECENT_MESSAGES_LIMIT = 8;

    private final ConversationMessageService conversationMessageService;

    public List<RecentConversationMessage> findRecentMessages(Long sessionId) {
        return conversationMessageService.findLatestBySessionId(sessionId, RECENT_MESSAGES_LIMIT)
                .stream()
                .map(this::toRecentMessage)
                .toList();
    }

    public String findLastBotMessage(List<RecentConversationMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            RecentConversationMessage message = messages.get(i);
            if (message.getDirection() == MessageDirection.OUTBOUND
                    && message.getSenderType() == SenderType.BOT) {
                return message.getContent();
            }
        }
        return null;
    }

    private RecentConversationMessage toRecentMessage(ConversationMessage message) {
        return RecentConversationMessage.builder()
                .direction(message.getDirection())
                .senderType(message.getSenderType())
                .content(message.getContent())
                .build();
    }
}
