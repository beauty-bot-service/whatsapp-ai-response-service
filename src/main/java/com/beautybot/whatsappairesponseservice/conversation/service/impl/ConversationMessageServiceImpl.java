package com.beautybot.whatsappairesponseservice.conversation.service.impl;

import com.beautybot.whatsappairesponseservice.conversation.state.MessageDirection;
import com.beautybot.whatsappairesponseservice.conversation.state.SenderType;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.persistence.repository.ConversationMessageModelRepository;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageServiceImpl implements ConversationMessageService {

    private static final String CHANNEL_HUMAN = "HUMAN";

    private final ConversationMessageModelRepository repository;

    @Override
    public boolean saveInbound(ConversationSession session, String content, String channel, String externalMessageId) {
        String normalizedChannel = normalizeChannel(channel);
        String normalizedExternalMessageId = normalizeExternalMessageId(externalMessageId);
        try {
            save(session, content, MessageDirection.INBOUND, SenderType.CUSTOMER, normalizedChannel, normalizedExternalMessageId);
            return true;
        } catch (DataIntegrityViolationException ex) {
            if (!isBlank(normalizedChannel) && !isBlank(normalizedExternalMessageId)) {
                log.info("Inbound duplicado detectado por constraint unica. channel={}, externalMessageId={}",
                        normalizedChannel, normalizedExternalMessageId);
                return false;
            }
            throw ex;
        }
    }

    @Override
    public void saveOutbound(ConversationSession session, String content) {
        save(session, content, MessageDirection.OUTBOUND, SenderType.BOT, null, null);
    }

    @Override
    public void saveHumanOutbound(ConversationSession session, String content) {
        save(session, content, MessageDirection.OUTBOUND, SenderType.HUMAN, CHANNEL_HUMAN, null);
    }

    @Override
    public List<ConversationMessage> findLatestBySessionId(Long sessionId, int limit) {
        return repository.findLatestBySessionId(sessionId, limit).stream()
                .sorted(Comparator.comparing(ConversationMessage::getCreatedAt))
                .toList();
    }

    @Override
    public boolean isInboundAlreadyProcessed(String channel, String externalMessageId) {
        String normalizedChannel = normalizeChannel(channel);
        String normalizedExternalMessageId = normalizeExternalMessageId(externalMessageId);
        if (isBlank(normalizedChannel) || isBlank(normalizedExternalMessageId)) {
            return false;
        }
        return repository.existsByChannelAndExternalMessageId(normalizedChannel, normalizedExternalMessageId);
    }

    private void save(
            ConversationSession session,
            String content,
            MessageDirection direction,
            SenderType senderType,
            String channel,
            String externalMessageId
    ) {
        repository.save(ConversationMessage.builder()
                .sessionId(session.getId())
                .phoneNumber(session.getPhoneNumber())
                .direction(direction)
                .senderType(senderType)
                .channel(channel)
                .externalMessageId(externalMessageId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeChannel(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeExternalMessageId(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}

