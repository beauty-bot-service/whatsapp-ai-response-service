package com.beautybot.whatsappairesponseservice.application.decision.context;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DecisionRulesProvider {

    public Map<ConversationState, String> allowedStates() {
        Map<ConversationState, String> states = new LinkedHashMap<>();
        states.put(ConversationState.COLLECTING_DATA, "El bot esta recolectando datos minimos del cliente antes de pasar la conversacion a una asesora.");
        states.put(ConversationState.READY_FOR_HUMAN, "Ya hay datos suficientes para que una asesora continue. El bot puede enviar un mensaje breve de cierre y luego debe dejar de responder.");
        states.put(ConversationState.HUMAN_HANDOFF, "El usuario pidio una persona, hizo una queja, realizo una consulta medica delicada, quiere cancelar/reprogramar, o el caso requiere intervencion humana. El bot puede enviar un mensaje breve de derivacion y luego debe dejar de responder.");
        states.put(ConversationState.CLOSED, "Conversacion cerrada. No debe continuar el flujo.");
        return states;
    }

    public List<Intent> allowedIntents() {
        return Arrays.asList(Intent.values());
    }

    public List<RequiredField> allowedWaitingFields() {
        return Arrays.asList(RequiredField.values());
    }

    public List<String> requiredLeadFields() {
        return List.of("treatmentInterest", "customerName", "firstTime", "preferredTime or contactPreference");
    }

    public List<String> mandatoryRules() {
        return List.of(
                "Return only valid JSON. Do not use markdown.",
                "Use Spanish with a serious, professional, human, administrative tone.",
                "Do not use emojis.",
                "Do not use exclamation marks.",
                "Do not use opening inverted question marks.",
                "Do not invent customer data.",
                "Extract only data clearly stated by the customer or clearly implied by the conversation.",
                "Do not overwrite existing session data with null.",
                "Do not ask again for information already present in currentSession.",
                "Ask for only one missing field at a time.",
                "If currentSession.waitingForField is not null, prioritize interpreting the current message as an answer to that field.",
                "If the customer chooses to be contacted by a human, set contactPreference to HUMAN_CONTACT and consider contact preference resolved.",
                "If all requiredLeadFields are complete, set nextState to READY_FOR_HUMAN.",
                "If nextState is READY_FOR_HUMAN, shouldCreateLead must be true and requiresHuman must be true.",
                "If the customer explicitly asks to speak with a human, complains, asks medical advice, or wants to cancel/reschedule, set nextState to HUMAN_HANDOFF.",
                "If nextState is HUMAN_HANDOFF, requiresHuman must be true and shouldNotifyHuman must be true.",
                "If the bot does not have calendar access, do not confirm appointments, dates, times, or availability.",
                "Interpret numeric dates as dd/MM Argentina.",
                "Use availabilitySuggestions for availability answers and never invent availability values.",
                "When informing availability, prefer grouped ranges instead of listing each 30-minute slot.",
                "If the bot does not have price access, do not provide exact prices; offer human follow-up.",
                "If the customer message contains multiple intents, handle the informational part briefly and continue the lead flow.",
                "If the current state is READY_FOR_HUMAN or HUMAN_HANDOFF, nextState must remain the same and shouldBotReply should usually be false.",
                "The reply field must contain the exact text to send when shouldBotReply is true. If shouldBotReply is false, reply must be null."
        );
    }
}
