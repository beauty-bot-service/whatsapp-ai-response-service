package com.beautybot.whatsappairesponseservice.ai;

import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;

public interface MessageAnalyzer {

    MessageAnalysis analyze(String message, ConversationSession session);
}


