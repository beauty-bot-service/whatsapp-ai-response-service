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

        // MVP: log-only notification. This can be replaced with internal WhatsApp, email, Slack, CRM, etc.
        log.info("New lead for advisor. phone={}, name={}, treatment={}, preferredTime={}, summary={}",
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

        log.info("New customer message in human-handoff conversation. phone={}, state={}, message={}",
                session.getPhoneNumber(),
                session.getState(),
                customerMessage);
    }
}

