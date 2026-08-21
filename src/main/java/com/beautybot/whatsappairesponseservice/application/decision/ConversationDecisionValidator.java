package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.ai.MedicalQuestionClassifier;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.ExtractedConversationData;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.ContactPreference;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConversationDecisionValidator {

    private static final int MAX_REPLY_LENGTH = 1000;
    private static final int MAX_SUMMARY_LENGTH = 2000;
    private static final int MAX_SHORT_FIELD_LENGTH = 120;

    public ConversationDecision validateAndFix(ConversationDecision decision, ConversationContext context) {
        if (decision == null) {
            decision = fallbackCollectingDecision(context, "Conversation engine returned a null decision.");
        }

        if (decision.getExtractedData() == null) {
            decision.setExtractedData(ExtractedConversationData.builder().build());
        }
        if (decision.getIntents() == null || decision.getIntents().isEmpty()) {
            decision.setIntents(List.of(Intent.UNKNOWN));
        }
        if (decision.getNextState() == null) {
            decision.setNextState(ConversationState.COLLECTING_DATA);
        }

        enforceMedicalHandoff(decision, context);

        boolean humanState = isHumanState(decision.getNextState());
        if (humanState) {
            decision.setRequiresHuman(true);
            decision.setShouldNotifyHuman(true);
            decision.setNextWaitingForField(null);
        }

        if (decision.getNextState() == ConversationState.READY_FOR_HUMAN && !hasEnoughLeadData(context.getCurrentSession(), decision)) {
            RequiredField missing = nextMissingField(context.getCurrentSession(), decision);
            decision.setNextState(ConversationState.COLLECTING_DATA);
            decision.setNextWaitingForField(missing);
            decision.setRequiresHuman(false);
            decision.setShouldCreateLead(false);
            decision.setShouldNotifyHuman(false);
            decision.setShouldBotReply(true);
            decision.setReply(fallbackQuestionFor(missing, context.getCurrentSession()));
            decision.setDecisionReason(appendReason(decision.getDecisionReason(), "Backend blocked READY_FOR_HUMAN because required lead data is missing."));
        }

        if (decision.getNextState() == ConversationState.READY_FOR_HUMAN) {
            decision.setShouldCreateLead(true);
        } else if (decision.getShouldCreateLead() == null) {
            decision.setShouldCreateLead(false);
        }

        if (decision.getRequiresHuman() == null) {
            decision.setRequiresHuman(false);
        }
        if (decision.getShouldNotifyHuman() == null) {
            decision.setShouldNotifyHuman(isHumanState(decision.getNextState()));
        }
        if (decision.getShouldBotReply() == null) {
            decision.setShouldBotReply(notBlank(decision.getReply()));
        }
        if (!Boolean.TRUE.equals(decision.getShouldBotReply())) {
            decision.setReply(null);
        }
        if (Boolean.TRUE.equals(decision.getShouldBotReply()) && !notBlank(decision.getReply())) {
            decision.setReply(fallbackReply(decision, context));
        }
        if (Boolean.TRUE.equals(decision.getShouldBotReply()) && notBlank(decision.getReply())) {
            decision.setReply(normalizeReplyStyle(decision.getReply()));
        }
        sanitizeDecisionText(decision);
        if (decision.getMissingFields() == null) {
            decision.setMissingFields(new ArrayList<>());
        }
        if (isHumanState(decision.getNextState()) && !notBlank(decision.getSummaryForHuman())) {
            decision.setSummaryForHuman(buildSummary(context.getCurrentSession(), decision));
        }
        sanitizeDecisionText(decision);
        if (!isHumanState(decision.getNextState()) && decision.getNextWaitingForField() == null && !hasEnoughLeadData(context.getCurrentSession(), decision)) {
            decision.setNextWaitingForField(nextMissingField(context.getCurrentSession(), decision));
        }

        return decision;
    }

    private void enforceMedicalHandoff(ConversationDecision decision, ConversationContext context) {
        String currentMessage = context == null || context.getCurrentMessage() == null
                ? null
                : context.getCurrentMessage().getMessage();
        boolean medicalIntent = decision.getIntents().contains(Intent.MEDICAL_QUESTION);
        if (!MedicalQuestionClassifier.requiresHuman(currentMessage)) {
            return;
        }
        if (!medicalIntent) {
            List<Intent> correctedIntents = new ArrayList<>(decision.getIntents());
            correctedIntents.addFirst(Intent.MEDICAL_QUESTION);
            decision.setIntents(correctedIntents);
        }
        decision.setNextState(ConversationState.HUMAN_HANDOFF);
        decision.setNextWaitingForField(null);
        decision.setRequiresHuman(true);
        decision.setShouldCreateLead(false);
        decision.setShouldNotifyHuman(true);
        decision.setShouldBotReply(true);
        decision.setReply("Para responderte con seguridad segun tu caso, derivo esta consulta con una profesional del equipo.");
        decision.setDecisionReason(appendReason(decision.getDecisionReason(),
                "Backend forced human handoff for a medical question."));
    }

    public boolean hasEnoughLeadData(ConversationSession session, ConversationDecision decision) {
        return notBlank(coalesce(extracted(decision).getTreatmentInterest(), session.getTreatmentInterest()))
                && notBlank(coalesce(extracted(decision).getCustomerName(), session.getCustomerName()))
                && coalesce(extracted(decision).getFirstTime(), session.getFirstTime()) != null
                && hasContactOrTime(session, decision);
    }

    public void applyExtractedDataToSession(ConversationSession session, ConversationDecision decision) {
        ExtractedConversationData data = extracted(decision);
        if (notBlank(data.getTreatmentInterest())) {
            session.setTreatmentInterest(truncate(data.getTreatmentInterest().trim(), MAX_SHORT_FIELD_LENGTH));
        }
        if (notBlank(data.getCustomerName())) {
            session.setCustomerName(truncate(data.getCustomerName().trim(), MAX_SHORT_FIELD_LENGTH));
        }
        if (data.getFirstTime() != null) {
            session.setFirstTime(data.getFirstTime());
        }
        if (notBlank(data.getPreferredTime())) {
            session.setPreferredTime(truncate(data.getPreferredTime().trim(), MAX_SHORT_FIELD_LENGTH));
        }
        if (data.getContactPreference() != null) {
            session.setContactPreference(data.getContactPreference());
            if (data.getContactPreference() == ContactPreference.HUMAN_CONTACT && !notBlank(session.getPreferredTime())) {
                session.setPreferredTime("Prefiere que una asesora lo contacte");
            }
        }
        session.setWaitingForField(decision.getNextWaitingForField());
    }

    private RequiredField nextMissingField(ConversationSession session, ConversationDecision decision) {
        ExtractedConversationData data = extracted(decision);
        if (!notBlank(coalesce(data.getTreatmentInterest(), session.getTreatmentInterest()))) {
            return RequiredField.TREATMENT;
        }
        if (!notBlank(coalesce(data.getCustomerName(), session.getCustomerName()))) {
            return RequiredField.NAME;
        }
        if (coalesce(data.getFirstTime(), session.getFirstTime()) == null) {
            return RequiredField.FIRST_TIME;
        }
        if (!hasContactOrTime(session, decision)) {
            return RequiredField.PREFERRED_TIME;
        }
        return null;
    }

    private boolean hasContactOrTime(ConversationSession session, ConversationDecision decision) {
        ExtractedConversationData data = extracted(decision);
        ContactPreference preference = data.getContactPreference() != null ? data.getContactPreference() : session.getContactPreference();
        return notBlank(coalesce(data.getPreferredTime(), session.getPreferredTime())) || preference != null;
    }

    private ConversationDecision fallbackCollectingDecision(ConversationContext context, String reason) {
        RequiredField field = context.getCurrentSession().getWaitingForField();
        if (field == null) {
            field = RequiredField.TREATMENT;
        }
        return ConversationDecision.builder()
                .intents(List.of(Intent.UNKNOWN))
                .nextState(ConversationState.COLLECTING_DATA)
                .nextWaitingForField(field)
                .extractedData(ExtractedConversationData.builder().build())
                .requiresHuman(false)
                .shouldCreateLead(false)
                .shouldNotifyHuman(false)
                .shouldBotReply(true)
                .reply(fallbackQuestionFor(field, context.getCurrentSession()))
                .decisionReason(reason)
                .build();
    }

    private String fallbackReply(ConversationDecision decision, ConversationContext context) {
        if (decision.getNextState() == ConversationState.HUMAN_HANDOFF) {
            return "Derivo tu consulta con una asesora para continuar con la atencion.";
        }
        if (decision.getNextState() == ConversationState.READY_FOR_HUMAN) {
            return "Perfecto. La consulta queda registrada y una asesora te contactara.";
        }
        return fallbackQuestionFor(decision.getNextWaitingForField(), context.getCurrentSession());
    }

    private String fallbackQuestionFor(RequiredField field, ConversationSession session) {
        if (field == RequiredField.NAME) {
            return "Perfecto. Por favor, indica tu nombre para dejarlo registrado.";
        }
        if (field == RequiredField.FIRST_TIME) {
            return "Por favor, confirma si es tu primera vez en la clinica?";
        }
        if (field == RequiredField.PREFERRED_TIME) {
            return "Gracias%s. Indica un dia u horario de preferencia. Si lo deseas, una asesora puede contactarte para coordinar."
                    .formatted(nameSuffix(session));
        }
        return "Hola. Indica que tratamiento te interesa consultar.";
    }

    private String buildSummary(ConversationSession session, ConversationDecision decision) {
        ExtractedConversationData data = extracted(decision);
        return "%s consulto por %s. Primera vez: %s. Preferencia/contacto: %s. Telefono: %s."
                .formatted(
                        emptyAsPending(coalesce(data.getCustomerName(), session.getCustomerName())),
                        emptyAsPending(coalesce(data.getTreatmentInterest(), session.getTreatmentInterest())),
                        coalesce(data.getFirstTime(), session.getFirstTime()) == null ? "pendiente" : Boolean.TRUE.equals(coalesce(data.getFirstTime(), session.getFirstTime())) ? "si" : "no",
                        emptyAsPending(resolvePreferenceText(session, data)),
                        session.getPhoneNumber()
                );
    }

    private String resolvePreferenceText(ConversationSession session, ExtractedConversationData data) {
        String preferredTime = coalesce(data.getPreferredTime(), session.getPreferredTime());
        if (notBlank(preferredTime)) {
            return preferredTime;
        }
        ContactPreference preference = data.getContactPreference() != null ? data.getContactPreference() : session.getContactPreference();
        if (preference == ContactPreference.HUMAN_CONTACT) {
            return "Prefiere que una asesora lo contacte";
        }
        return null;
    }

    private String emptyAsPending(String value) {
        return notBlank(value) ? value : "pendiente";
    }

    private String nameSuffix(ConversationSession session) {
        return notBlank(session.getCustomerName()) ? ", " + session.getCustomerName() : "";
    }

    private ExtractedConversationData extracted(ConversationDecision decision) {
        return decision.getExtractedData() == null ? ExtractedConversationData.builder().build() : decision.getExtractedData();
    }

    private boolean isHumanState(ConversationState state) {
        return state == ConversationState.READY_FOR_HUMAN || state == ConversationState.HUMAN_HANDOFF;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }

    private String appendReason(String current, String extra) {
        if (!notBlank(current)) {
            return extra;
        }
        return current + " " + extra;
    }

    private void sanitizeDecisionText(ConversationDecision decision) {
        if (decision == null) {
            return;
        }
        if (decision.getReply() != null) {
            decision.setReply(truncate(decision.getReply().trim(), MAX_REPLY_LENGTH));
        }
        if (decision.getSummaryForHuman() != null) {
            decision.setSummaryForHuman(truncate(decision.getSummaryForHuman().trim(), MAX_SUMMARY_LENGTH));
        }
        if (decision.getDecisionReason() != null) {
            decision.setDecisionReason(truncate(decision.getDecisionReason().trim(), MAX_SUMMARY_LENGTH));
        }
        ExtractedConversationData data = decision.getExtractedData();
        if (data != null) {
            data.setCustomerName(truncateOrNull(data.getCustomerName(), MAX_SHORT_FIELD_LENGTH));
            data.setTreatmentInterest(truncateOrNull(data.getTreatmentInterest(), MAX_SHORT_FIELD_LENGTH));
            data.setPreferredTime(truncateOrNull(data.getPreferredTime(), MAX_SHORT_FIELD_LENGTH));
        }
    }

    private String truncateOrNull(String value, int maxLength) {
        if (!notBlank(value)) {
            return null;
        }
        return truncate(value.trim(), maxLength);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String normalizeReplyStyle(String reply) {
        String normalized = reply
                .replace("\u00BF", "")
                .replace("\u00A1", "")
                .replace("!", "");
        return normalized.replaceAll("\\s{2,}", " ").trim();
    }
}
