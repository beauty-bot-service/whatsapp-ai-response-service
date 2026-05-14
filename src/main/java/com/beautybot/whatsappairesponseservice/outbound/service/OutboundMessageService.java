package com.beautybot.whatsappairesponseservice.outbound.service;

import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.outbound.model.OutboundMessage;

public interface OutboundMessageService {
    OutboundMessage sendBotReply(ConversationSession session, String content);
}
