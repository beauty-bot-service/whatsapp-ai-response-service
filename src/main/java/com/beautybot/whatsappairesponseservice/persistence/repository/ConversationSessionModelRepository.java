package com.beautybot.whatsappairesponseservice.persistence.repository;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.persistence.dao.ConversationSessionDao;
import com.beautybot.whatsappairesponseservice.persistence.mapper.ConversationSessionEntityMapper;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConversationSessionModelRepository {

    private final ConversationSessionDao dao;
    private final ConversationSessionEntityMapper mapper;

    public Optional<ConversationSession> findLatestReusableByPhoneNumber(
            String phoneNumber,
            LocalDateTime collectingUpdatedAfter,
            LocalDateTime readyForHumanUpdatedAfter,
            LocalDateTime humanHandoffUpdatedAfter
    ) {
        return dao.findReusableByPhoneNumber(
                        phoneNumber,
                        ConversationState.COLLECTING_DATA,
                        collectingUpdatedAfter,
                        ConversationState.READY_FOR_HUMAN,
                        readyForHumanUpdatedAfter,
                        ConversationState.HUMAN_HANDOFF,
                        humanHandoffUpdatedAfter,
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .map(mapper::toModel);
    }

    public ConversationSession save(ConversationSession session) {
        return mapper.toModel(dao.save(mapper.toEntity(session)));
    }
}


