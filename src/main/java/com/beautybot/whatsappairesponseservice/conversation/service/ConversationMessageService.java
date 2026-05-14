package com.beautybot.whatsappairesponseservice.conversation.service;

import com.beautybot.whatsappairesponseservice.conversation.model.ConversationMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;

import java.util.List;

public interface ConversationMessageService {

    boolean saveInbound(ConversationSession session, String content, String channel, String externalMessageId);

    void saveOutbound(ConversationSession session, String content);

    void saveHumanOutbound(ConversationSession session, String content);

    boolean isInboundAlreadyProcessed(String channel, String externalMessageId);

    List<ConversationMessage> findLatestBySessionId(Long sessionId, int limit);
}

