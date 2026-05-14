package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.ai.MessageAnalyzer;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.ExtractedConversationData;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import com.beautybot.whatsappairesponseservice.conversation.policy.HandoffPolicy;
import com.beautybot.whatsappairesponseservice.conversation.reply.BotResponseService;
import com.beautybot.whatsappairesponseservice.conversation.resolver.MissingDataResolver;
import com.beautybot.whatsappairesponseservice.conversation.state.ContactPreference;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuleBasedConversationDecisionService implements ConversationDecisionService {

    private final MessageAnalyzer messageAnalyzer;
    private final HandoffPolicy handoffPolicy;
    private final MissingDataResolver missingDataResolver;
    private final BotResponseService responseService;

    @Override
    public ConversationDecision decide(ConversationContext context) {
        ConversationSession session = context.getCurrentSession();
        MessageAnalysis analysis = messageAnalyzer.analyze(context.getCurrentMessage().getMessage(), session);
        enrichSessionWithAnalysis(session, analysis);

        ConversationDecision decision = resolveDecision(session, analysis);
        enrichDecisionWithExtractedData(decision, analysis);
        return decision;
    }

    private ConversationDecision resolveDecision(ConversationSession session, MessageAnalysis analysis) {
        if (handoffPolicy.shouldHandoffImmediately(analysis)) {
            session.setWaitingForField(null);
            return responseService.handoffToHuman(session, analysis);
        }

        boolean hasInformationalIntent = hasInformationalIntent(analysis);
        boolean hasAppointmentIntent = analysis.hasIntent(Intent.APPOINTMENT_REQUEST);

        if (hasInformationalIntent && !hasAppointmentIntent) {
            return responseService.answerInformational(analysis);
        }

        ConversationDecision collectionDecision = resolveLeadCollection(session, analysis);
        if (hasInformationalIntent) {
            ConversationDecision informationalDecision = responseService.answerInformational(analysis);
            return responseService.mergeReplies(collectionDecision, informationalDecision.getReply());
        }
        return collectionDecision;
    }

    private ConversationDecision resolveLeadCollection(ConversationSession session, MessageAnalysis analysis) {
        Optional<RequiredField> missingField = missingDataResolver.nextMissingField(session);
        if (missingField.isPresent()) {
            session.setWaitingForField(missingField.get());
            return responseService.askFor(missingField.get(), session, analysis);
        }
        session.setWaitingForField(null);
        return responseService.readyForHuman(session, analysis);
    }

    private void enrichDecisionWithExtractedData(ConversationDecision decision, MessageAnalysis analysis) {
        decision.setExtractedData(ExtractedConversationData.builder()
                .customerName(analysis.getExtractedName())
                .treatmentInterest(analysis.getTreatment())
                .firstTime(analysis.getFirstTime())
                .preferredTime(analysis.getPreferredTime())
                .contactPreference(resolveContactPreference(analysis.getPreferredTime()))
                .build());
    }

    private ContactPreference resolveContactPreference(String preferredTime) {
        if (preferredTime != null && preferredTime.toLowerCase().contains("asesora")) {
            return ContactPreference.HUMAN_CONTACT;
        }
        return null;
    }

    private boolean hasInformationalIntent(MessageAnalysis analysis) {
        return analysis.hasIntent(Intent.LOCATION_QUESTION)
                || analysis.hasIntent(Intent.OPENING_HOURS_QUESTION)
                || analysis.hasIntent(Intent.PRICE_QUESTION)
                || analysis.hasIntent(Intent.AVAILABILITY_QUESTION);
    }

    private void enrichSessionWithAnalysis(ConversationSession session, MessageAnalysis analysis) {
        if (hasText(analysis.getTreatment())) {
            session.setTreatmentInterest(analysis.getTreatment());
        }
        if (hasText(analysis.getExtractedName())) {
            session.setCustomerName(analysis.getExtractedName());
        }
        if (analysis.getFirstTime() != null) {
            session.setFirstTime(analysis.getFirstTime());
        }
        if (hasText(analysis.getPreferredTime())) {
            session.setPreferredTime(analysis.getPreferredTime());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
