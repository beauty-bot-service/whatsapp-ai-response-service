package com.beautybot.whatsappairesponseservice.outbound.notification.impl;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.observability.PhoneNumberMasker;
import com.beautybot.whatsappairesponseservice.outbound.notification.HumanNotificationService;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppCloudApiClient;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanNotificationServiceImpl implements HumanNotificationService {

    private final BeautyBotProperties properties;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final PhoneNumberMasker phoneNumberMasker;

    @Override
    public boolean notifyAdvisor(ConversationSession session) {
        if (!properties.isAdvisorNotificationEnabled()) {
            return false;
        }

        return sendAdvisorNotification(buildLeadNotification(session), "lead", session);
    }

    @Override
    public boolean notifyAdvisorAboutNewCustomerMessage(ConversationSession session, String customerMessage) {
        if (!properties.isAdvisorNotificationEnabled()) {
            return false;
        }

        return sendAdvisorNotification(buildCustomerMessageNotification(session, customerMessage), "customer_message", session);
    }

    private boolean sendAdvisorNotification(String message, String type, ConversationSession session) {
        String advisorPhone = properties.getAdvisorNotificationPhoneNumber();
        if (advisorPhone == null || advisorPhone.isBlank()) {
            log.error("Advisor notification is enabled but advisor phone number is missing. type={}, sessionId={}",
                    type, session == null ? null : session.getId());
            return false;
        }

        WhatsAppSendResult result = whatsAppCloudApiClient.sendTextMessage(advisorPhone.trim(), message);
        if (result == WhatsAppSendResult.SENT) {
            log.info("Advisor notification sent. type={}, sessionId={}, customerPhone={}",
                    type,
                    session == null ? null : session.getId(),
                    session == null ? null : phoneNumberMasker.mask(session.getPhoneNumber()));
            return true;
        }

        log.warn("Advisor notification failed. type={}, sessionId={}, customerPhone={}, result={}",
                type,
                session == null ? null : session.getId(),
                session == null ? null : phoneNumberMasker.mask(session.getPhoneNumber()),
                result);
        return false;
    }

    private String buildLeadNotification(ConversationSession session) {
        return String.join("\n",
                "Nuevo lead para asesor",
                "Telefono: " + valueOrPending(session.getPhoneNumber()),
                "Nombre: " + valueOrPending(session.getCustomerName()),
                "Tratamiento: " + valueOrPending(session.getTreatmentInterest()),
                "Primera vez: " + firstTimeText(session.getFirstTime()),
                "Preferencia: " + valueOrPending(session.getPreferredTime()),
                "Resumen: " + valueOrPending(session.getSummaryForHuman())
        );
    }

    private String buildCustomerMessageNotification(ConversationSession session, String customerMessage) {
        return String.join("\n",
                "Nuevo mensaje de cliente en conversacion con asesora",
                "Telefono: " + valueOrPending(session.getPhoneNumber()),
                "Estado: " + valueOrPending(session.getState() == null ? null : session.getState().name()),
                "Mensaje: " + valueOrPending(customerMessage)
        );
    }

    private String firstTimeText(Boolean firstTime) {
        if (firstTime == null) {
            return "pendiente";
        }
        return Boolean.TRUE.equals(firstTime) ? "si" : "no";
    }

    private String valueOrPending(String value) {
        return value == null || value.isBlank() ? "pendiente" : value.trim();
    }
}
