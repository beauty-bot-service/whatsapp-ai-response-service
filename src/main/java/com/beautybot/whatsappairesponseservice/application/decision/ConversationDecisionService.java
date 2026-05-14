package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;

public interface ConversationDecisionService {
    ConversationDecision decide(ConversationContext context);
}
