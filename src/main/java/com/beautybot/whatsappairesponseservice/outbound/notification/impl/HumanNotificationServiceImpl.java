package com.beautybot.whatsappairesponseservice.outbound.notification.impl;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.outbound.notification.HumanNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanNotificationServiceImpl implements HumanNotificationService {

    private final BeautyBotProperties properties;

    @Override
    public void notifyAdvisor(ConversationSession session) {
        if (!properties.isAdvisorNotificationEnabled()) {
            return;
        }

        // MVP: por ahora solo log. Despues esto puede ser WhatsApp interno, email, Slack, CRM, etc.
        log.info("Nuevo lead para asesora. phone={}, name={}, treatment={}, preferredTime={}, summary={}",
                session.getPhoneNumber(),
                session.getCustomerName(),
                session.getTreatmentInterest(),
                session.getPreferredTime(),
                session.getSummaryForHuman());
    }

    @Override
    public void notifyAdvisorAboutNewCustomerMessage(ConversationSession session, String customerMessage) {
        if (!properties.isAdvisorNotificationEnabled()) {
            return;
        }

        log.info("Nuevo mensaje de cliente en conversacion derivada. phone={}, state={}, message={}",
                session.getPhoneNumber(),
                session.getState(),
                customerMessage);
    }
}


