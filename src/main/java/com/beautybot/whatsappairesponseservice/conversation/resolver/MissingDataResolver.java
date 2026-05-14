package com.beautybot.whatsappairesponseservice.conversation.resolver;

import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MissingDataResolver {

    public Optional<RequiredField> nextMissingField(ConversationSession session) {
        if (isBlank(session.getTreatmentInterest())) {
            return Optional.of(RequiredField.TREATMENT);
        }
        if (isBlank(session.getCustomerName())) {
            return Optional.of(RequiredField.NAME);
        }
        if (session.getFirstTime() == null) {
            return Optional.of(RequiredField.FIRST_TIME);
        }
        if (isBlank(session.getPreferredTime())) {
            return Optional.of(RequiredField.PREFERRED_TIME);
        }
        return Optional.empty();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}


