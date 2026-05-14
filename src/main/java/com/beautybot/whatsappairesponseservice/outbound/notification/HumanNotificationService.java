package com.beautybot.whatsappairesponseservice.outbound.notification;

import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;

public interface HumanNotificationService {

    void notifyAdvisor(ConversationSession session);

    void notifyAdvisorAboutNewCustomerMessage(ConversationSession session, String customerMessage);
}


