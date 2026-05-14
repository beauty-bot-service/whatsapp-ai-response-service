package com.beautybot.whatsappairesponseservice.persistence.dao;

import com.beautybot.whatsappairesponseservice.persistence.entity.ConversationProcessingLockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationProcessingLockDao extends JpaRepository<ConversationProcessingLockEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l
            FROM ConversationProcessingLockEntity l
            WHERE l.phoneNumber = :phoneNumber
            """)
    Optional<ConversationProcessingLockEntity> findByPhoneNumberForUpdate(@Param("phoneNumber") String phoneNumber);
}
