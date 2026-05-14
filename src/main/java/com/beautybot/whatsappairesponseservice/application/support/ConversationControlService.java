package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationDatabaseLockService;
import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationProcessingLockService;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ConversationControlService {

    private final ConversationService conversationService;
    private final ConversationMessageHistoryService messageHistoryService;
    private final ConversationDatabaseLockService conversationDatabaseLockService;
    private final ConversationProcessingLockService conversationProcessingLockService;
    private final TransactionTemplate transactionTemplate;

    public ConversationSession takeoverByHuman(String phoneNumber, String humanMessage) {
        String normalizedPhone = normalizePhone(phoneNumber);
        return conversationProcessingLockService.executeLocked(normalizedPhone, () ->
                transactionTemplate.execute(status -> {
                    conversationDatabaseLockService.lockPhoneNumber(normalizedPhone);
                    ConversationSession session = conversationService.getOrCreate(normalizedPhone);
                    conversationService.markHumanTakeover(session);
                    if (hasText(humanMessage)) {
                        messageHistoryService.saveHumanOutbound(session, humanMessage.trim());
                    }
                    return session;
                })
        );
    }

    public ConversationSession releaseToBot(String phoneNumber) {
        String normalizedPhone = normalizePhone(phoneNumber);
        return conversationProcessingLockService.executeLocked(normalizedPhone, () ->
                transactionTemplate.execute(status -> {
                    conversationDatabaseLockService.lockPhoneNumber(normalizedPhone);
                    ConversationSession session = conversationService.getOrCreate(normalizedPhone);
                    conversationService.releaseHumanTakeover(session);
                    return session;
                })
        );
    }

    private String normalizePhone(String value) {
        if (value == null) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        String normalized = value.trim().replace(" ", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
