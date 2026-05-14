package com.beautybot.whatsappairesponseservice.persistence.dao;

import com.beautybot.whatsappairesponseservice.persistence.entity.ConversationMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageDao extends JpaRepository<ConversationMessageEntity, Long> {

    boolean existsByChannelAndExternalMessageId(String channel, String externalMessageId);

    List<ConversationMessageEntity> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
}



