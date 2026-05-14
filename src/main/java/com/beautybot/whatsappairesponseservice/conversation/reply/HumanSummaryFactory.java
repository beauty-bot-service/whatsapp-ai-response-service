package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import org.springframework.stereotype.Component;

@Component
public class HumanSummaryFactory {

    public String build(ConversationSession session) {
        return "%s consulto por %s. Primera vez: %s. Preferencia/contacto: %s. Telefono: %s."
                .formatted(
                        pending(session.getCustomerName()),
                        pending(session.getTreatmentInterest()),
                        session.getFirstTime() == null ? "pendiente" : Boolean.TRUE.equals(session.getFirstTime()) ? "si" : "no",
                        pending(resolvePreference(session)),
                        session.getPhoneNumber()
                );
    }

    private String resolvePreference(ConversationSession session) {
        if (hasText(session.getPreferredTime())) {
            return session.getPreferredTime();
        }
        if (session.getContactPreference() != null) {
            return session.getContactPreference().name();
        }
        return null;
    }

    private String pending(String value) {
        return hasText(value) ? value.trim() : "pendiente";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
