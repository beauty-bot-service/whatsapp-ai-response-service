package com.beautybot.whatsappairesponseservice.application.usecase;

import com.beautybot.whatsappairesponseservice.application.ChatService;
import com.beautybot.whatsappairesponseservice.application.decision.ConversationContextBuilder;
import com.beautybot.whatsappairesponseservice.application.decision.ConversationDecisionRouter;
import com.beautybot.whatsappairesponseservice.application.decision.ConversationDecisionValidator;
import com.beautybot.whatsappairesponseservice.application.support.ChatMessageValidator;
import com.beautybot.whatsappairesponseservice.application.support.ClinicIdProvider;
import com.beautybot.whatsappairesponseservice.application.support.ConversationMessageHistoryService;
import com.beautybot.whatsappairesponseservice.application.support.HumanHandoffService;
import com.beautybot.whatsappairesponseservice.application.support.InboundMessageNormalizer;
import com.beautybot.whatsappairesponseservice.application.promotion.ConversationPromotionPolicy;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationDatabaseLockService;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatResult;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationService;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.lead.dto.LeadUpsertRequest;
import com.beautybot.whatsappairesponseservice.lead.model.LeadSource;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.service.LeadService;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandleIncomingMessageUseCase implements ChatService {
    private final ConversationService conversationService;
    private final ConversationMessageHistoryService messageHistoryService;
    private final LeadService leadService;
    private final HumanHandoffService humanHandoffService;
    private final InboundMessageNormalizer inboundMessageNormalizer;
    private final ChatMessageValidator chatMessageValidator;
    private final ConversationContextBuilder conversationContextBuilder;
    private final ConversationDecisionRouter conversationDecisionRouter;
    private final ConversationDecisionValidator conversationDecisionValidator;
    private final ConversationPromotionPolicy conversationPromotionPolicy;
    private final ClinicIdProvider clinicIdProvider;
    private final TransactionTemplate transactionTemplate;
    private final BeautyBotMetrics metrics;
    private final ConversationDatabaseLockService conversationDatabaseLockService;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResult handleMessage(ChatMessage request) {
        ChatMessage normalizedRequest = inboundMessageNormalizer.normalize(request);
        chatMessageValidator.validateNormalized(normalizedRequest);

        if (messageHistoryService.isInboundAlreadyProcessed(normalizedRequest)) {
            metrics.inboundMessage(normalizedRequest.getChannel(), "duplicate_precheck");
            return ChatResult.builder()
                    .reply(null)
                    .requiresHuman(false)
                    .build();
        }

        InboundProcessingResult inboundResult = transactionTemplate.execute(status -> persistInbound(normalizedRequest));
        if (inboundResult == null || !inboundResult.persisted()) {
            metrics.inboundMessage(normalizedRequest.getChannel(), "duplicate_constraint");
            ConversationSession session = inboundResult == null ? null : inboundResult.session();
            return ChatResult.builder()
                    .reply(null)
                    .state(session == null ? null : session.getState())
                    .requiresHuman(session != null && Boolean.TRUE.equals(session.getRequiresHuman()))
                    .session(session)
                    .build();
        }

        ConversationSession session = inboundResult.session();
        if (humanHandoffService.isWaitingForHuman(session)) {
            transactionTemplate.executeWithoutResult(status -> {
                conversationService.markCustomerMessageNow(session);
                humanHandoffService.notifyAdvisorAboutNewCustomerMessage(session, normalizedRequest.getMessage());
            });
            return ChatResult.builder()
                    .reply(null)
                    .state(session.getState())
                    .requiresHuman(true)
                    .session(session)
                    .build();
        }

        ConversationContext context = conversationContextBuilder.build(session, normalizedRequest);
        ConversationDecision routedDecision = conversationDecisionRouter.decide(context);

        return transactionTemplate.execute(status -> {
            conversationDatabaseLockService.lockPhoneNumber(normalizedRequest.getPhoneNumber());
            ConversationDecision decision = conversationPromotionPolicy.enrich(
                    clinicIdProvider.currentClinicId(),
                    context,
                    routedDecision
            );
            return applyDecisionAndPersistResult(session, decision);
        });
    }

    private InboundProcessingResult persistInbound(ChatMessage normalizedRequest) {
        conversationDatabaseLockService.lockPhoneNumber(normalizedRequest.getPhoneNumber());
        ConversationSession session = conversationService.getOrCreate(normalizedRequest.getPhoneNumber());
        boolean persisted = messageHistoryService.saveInbound(session, normalizedRequest);
        return new InboundProcessingResult(session, persisted);
    }

    private ChatResult applyDecisionAndPersistResult(ConversationSession session, ConversationDecision decision) {
        ConversationState previousState = session.getState();

        conversationDecisionValidator.applyExtractedDataToSession(session, decision);
        conversationService.applyDecision(session, decision);
        humanHandoffService.notifyAdvisorIfTransitioned(previousState, decision, session);

        if (Boolean.TRUE.equals(decision.getShouldBotReply()) && hasText(decision.getReply())) {
            messageHistoryService.saveOutbound(session, decision.getReply());
        }

        safeUpsertLead(session, decision);

        if (Boolean.TRUE.equals(decision.getShouldNotifyHuman())) {
            metrics.handoff("notified");
        }

        return ChatResult.builder()
                .reply(Boolean.TRUE.equals(decision.getShouldBotReply()) ? decision.getReply() : null)
                .state(decision.getNextState())
                .requiresHuman(decision.getRequiresHuman())
                .session(session)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void safeUpsertLead(ConversationSession session, ConversationDecision decision) {
        try {
            leadService.upsertFromConversation(LeadUpsertRequest.builder()
                    .clinicId(clinicIdProvider.currentClinicId())
                    .conversationSessionId(session.getId())
                    .phoneNumber(session.getPhoneNumber())
                    .customerName(session.getCustomerName())
                    .treatmentInterest(session.getTreatmentInterest())
                    .firstTime(session.getFirstTime())
                    .preferredTime(session.getPreferredTime())
                    .source(LeadSource.WHATSAPP_INBOUND)
                    .readyForHuman(isReadyForHuman(decision))
                    .appointmentRequested(isAppointmentRequested(decision))
                    .suggestedStatus(resolveSuggestedStatus(session, decision))
                    .metadata(buildLeadMetadata(decision))
                    .build());
        } catch (Exception e) {
            log.error(
                    "Error updating lead. phoneNumber={}, sessionId={}",
                    session.getPhoneNumber(),
                    session.getId(),
                    e
            );
        }
    }

    private boolean isReadyForHuman(ConversationDecision decision) {
        ConversationState nextState = decision == null ? null : decision.getNextState();
        return nextState == ConversationState.READY_FOR_HUMAN || nextState == ConversationState.HUMAN_HANDOFF;
    }

    private boolean isAppointmentRequested(ConversationDecision decision) {
        return decision != null
                && decision.getIntents() != null
                && decision.getIntents().stream().anyMatch(intent -> intent == Intent.APPOINTMENT_REQUEST);
    }

    private LeadStatus resolveSuggestedStatus(ConversationSession session, ConversationDecision decision) {
        if (isAppointmentRequested(decision)) {
            return LeadStatus.APPOINTMENT_REQUESTED;
        }
        if (isReadyForHuman(decision)) {
            return LeadStatus.READY_FOR_HUMAN;
        }
        boolean hasCommercialData = hasText(session.getTreatmentInterest())
                || hasText(session.getCustomerName())
                || session.getFirstTime() != null
                || hasText(session.getPreferredTime());
        return hasCommercialData ? LeadStatus.QUALIFYING : LeadStatus.NEW;
    }

    private String buildLeadMetadata(ConversationDecision decision) {
        if (decision == null) {
            return null;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("intents", decision.getIntents());
        metadata.put("nextState", decision.getNextState());
        metadata.put("decisionSource", decision.getSource());
        metadata.put("matchedPromotionCodes", decision.getMatchedPromotionCodes());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private record InboundProcessingResult(ConversationSession session, boolean persisted) {
    }
}
