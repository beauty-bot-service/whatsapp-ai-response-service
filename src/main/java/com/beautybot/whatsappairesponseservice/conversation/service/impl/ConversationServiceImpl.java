package com.beautybot.whatsappairesponseservice.conversation.service.impl;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.persistence.repository.ConversationSessionModelRepository;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationService;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private static final int DEFAULT_COLLECTING_REUSE_HOURS = 24;
    private static final int DEFAULT_READY_FOR_HUMAN_REUSE_HOURS = 168;
    private static final int DEFAULT_HUMAN_HANDOFF_REUSE_HOURS = 168;

    private final ConversationSessionModelRepository repository;
    private final BeautyBotProperties properties;

    @Override
    public ConversationSession getOrCreate(String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime collectingUpdatedAfter = now.minusHours(resolveCollectingReuseHours());
        LocalDateTime readyForHumanUpdatedAfter = now.minusHours(resolveReadyForHumanReuseHours());
        LocalDateTime humanHandoffUpdatedAfter = now.minusHours(resolveHumanHandoffReuseHours());

        return repository.findLatestReusableByPhoneNumber(
                        phoneNumber,
                        collectingUpdatedAfter,
                        readyForHumanUpdatedAfter,
                        humanHandoffUpdatedAfter
                )
                .orElseGet(() -> createNewSession(phoneNumber));
    }

    @Override
    public void applyDecision(ConversationSession session, ConversationDecision decision) {
        session.setState(decision.getNextState());
        session.setRequiresHuman(Boolean.TRUE.equals(decision.getRequiresHuman()));
        session.setSummaryForHuman(decision.getSummaryForHuman());
        session.setUpdatedAt(LocalDateTime.now());
        repository.save(session);
    }

    @Override
    public void markCustomerMessageNow(ConversationSession session) {
        session.setUpdatedAt(LocalDateTime.now());
        repository.save(session);
    }

    @Override
    public void markHumanNotifiedNow(ConversationSession session) {
        session.setHumanNotifiedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        repository.save(session);
    }

    @Override
    public void markHumanTakeover(ConversationSession session) {
        LocalDateTime now = LocalDateTime.now();
        session.setState(ConversationState.HUMAN_HANDOFF);
        session.setRequiresHuman(true);
        session.setWaitingForField(null);
        if (session.getHumanNotifiedAt() == null) {
            session.setHumanNotifiedAt(now);
        }
        session.setUpdatedAt(now);
        repository.save(session);
    }

    @Override
    public void releaseHumanTakeover(ConversationSession session) {
        session.setState(ConversationState.COLLECTING_DATA);
        session.setRequiresHuman(false);
        session.setWaitingForField(null);
        session.setUpdatedAt(LocalDateTime.now());
        repository.save(session);
    }

    private ConversationSession createNewSession(String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();
        ConversationSession session = ConversationSession.builder()
                .phoneNumber(phoneNumber)
                .state(ConversationState.COLLECTING_DATA)
                .requiresHuman(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return repository.save(session);
    }

    private int resolveCollectingReuseHours() {
        Integer stateSpecific = conversationProperties().getCollectingReuseHours();
        Integer legacy = properties.getConversationSessionReuseHours();
        return resolveReuseHours(stateSpecific, legacy, DEFAULT_COLLECTING_REUSE_HOURS);
    }

    private int resolveReadyForHumanReuseHours() {
        Integer stateSpecific = conversationProperties().getReadyForHumanReuseHours();
        Integer legacy = properties.getConversationSessionReuseHours();
        return resolveReuseHours(stateSpecific, legacy, DEFAULT_READY_FOR_HUMAN_REUSE_HOURS);
    }

    private int resolveHumanHandoffReuseHours() {
        Integer stateSpecific = conversationProperties().getHumanHandoffReuseHours();
        Integer legacy = properties.getConversationSessionReuseHours();
        return resolveReuseHours(stateSpecific, legacy, DEFAULT_HUMAN_HANDOFF_REUSE_HOURS);
    }

    private int resolveReuseHours(Integer stateSpecificValue, Integer legacyValue, int fallbackDefault) {
        if (isPositive(stateSpecificValue)) {
            return stateSpecificValue;
        }
        if (isPositive(legacyValue)) {
            return legacyValue;
        }
        return fallbackDefault;
    }

    private boolean isPositive(Integer value) {
        return value != null && value > 0;
    }

    private BeautyBotProperties.Conversation conversationProperties() {
        BeautyBotProperties.Conversation conversation = properties.getConversation();
        return conversation != null ? conversation : new BeautyBotProperties.Conversation();
    }
}
