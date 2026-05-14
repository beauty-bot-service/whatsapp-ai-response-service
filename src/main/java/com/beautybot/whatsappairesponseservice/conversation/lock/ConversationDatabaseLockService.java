package com.beautybot.whatsappairesponseservice.conversation.lock;

import com.beautybot.whatsappairesponseservice.persistence.dao.ConversationProcessingLockDao;
import com.beautybot.whatsappairesponseservice.persistence.entity.ConversationProcessingLockEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConversationDatabaseLockService {

    private final ConversationProcessingLockDao dao;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = normalize(phoneNumber);
        if (dao.findByPhoneNumberForUpdate(normalizedPhoneNumber).isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        try {
            dao.saveAndFlush(ConversationProcessingLockEntity.builder()
                    .phoneNumber(normalizedPhoneNumber)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // Another transaction inserted the lock row first. Continue by selecting it FOR UPDATE.
        }
        dao.findByPhoneNumberForUpdate(normalizedPhoneNumber)
                .orElseThrow(() -> new IllegalStateException("Could not acquire conversation lock"));
    }

    private String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "unknown";
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        return digits.isBlank() ? phoneNumber.trim() : digits;
    }
}
