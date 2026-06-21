package com.beautybot.whatsappairesponseservice.outbound.notification;

import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;

public interface HumanNotificationService {

    boolean notifyAdvisor(ConversationSession session);

    boolean notifyAdvisorAboutNewCustomerMessage(ConversationSession session, String customerMessage);
}

