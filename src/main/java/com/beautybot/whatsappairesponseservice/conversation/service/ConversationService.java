package com.beautybot.whatsappairesponseservice.conversation.service;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;

public interface ConversationService {

    ConversationSession getOrCreate(String phoneNumber);

    void applyDecision(ConversationSession session, ConversationDecision decision);

    void markCustomerMessageNow(ConversationSession session);

    void markHumanNotifiedNow(ConversationSession session);

    void markHumanTakeover(ConversationSession session);

    void releaseHumanTakeover(ConversationSession session);
}

