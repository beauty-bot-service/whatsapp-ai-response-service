package com.beautybot.whatsappairesponseservice.persistence.repository;

import com.beautybot.whatsappairesponseservice.persistence.dao.ConversationMessageDao;
import com.beautybot.whatsappairesponseservice.persistence.mapper.ConversationMessageEntityMapper;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConversationMessageModelRepository {

    private final ConversationMessageDao dao;
    private final ConversationMessageEntityMapper mapper;

    public ConversationMessage save(ConversationMessage message) {
        return mapper.toModel(dao.save(mapper.toEntity(message)));
    }

    public boolean existsByChannelAndExternalMessageId(String channel, String externalMessageId) {
        return dao.existsByChannelAndExternalMessageId(channel, externalMessageId);
    }

    public List<ConversationMessage> findLatestBySessionId(Long sessionId, int limit) {
        return dao.findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(mapper::toModel)
                .toList();
    }
}



