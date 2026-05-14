package com.beautybot.whatsappairesponseservice.persistence.dao;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.persistence.entity.ConversationSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationSessionDao extends JpaRepository<ConversationSessionEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM ConversationSessionEntity s
            WHERE s.phoneNumber = :phoneNumber
              AND (
                (s.state = :collectingState AND s.updatedAt > :collectingUpdatedAfter)
                OR (s.state = :readyForHumanState AND s.updatedAt > :readyForHumanUpdatedAfter)
                OR (s.state = :humanHandoffState AND s.updatedAt > :humanHandoffUpdatedAfter)
              )
            ORDER BY s.updatedAt DESC
            """)
    List<ConversationSessionEntity> findReusableByPhoneNumber(
            @Param("phoneNumber") String phoneNumber,
            @Param("collectingState") ConversationState collectingState,
            @Param("collectingUpdatedAfter") LocalDateTime collectingUpdatedAfter,
            @Param("readyForHumanState") ConversationState readyForHumanState,
            @Param("readyForHumanUpdatedAfter") LocalDateTime readyForHumanUpdatedAfter,
            @Param("humanHandoffState") ConversationState humanHandoffState,
            @Param("humanHandoffUpdatedAfter") LocalDateTime humanHandoffUpdatedAfter,
            Pageable pageable
    );
}
