package com.beautybot.whatsappairesponseservice.conversation.service.impl;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import com.beautybot.whatsappairesponseservice.persistence.repository.ConversationSessionModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationSessionModelRepository repository;

    @Mock
    private BeautyBotProperties properties;

    @InjectMocks
    private ConversationServiceImpl service;

    @Test
    void usesStateSpecificWindowsWhenConfigured() {
        String phoneNumber = "5491112345678";
        BeautyBotProperties.Conversation conversation = new BeautyBotProperties.Conversation();
        conversation.setCollectingReuseHours(24);
        conversation.setReadyForHumanReuseHours(168);
        conversation.setHumanHandoffReuseHours(168);

        ConversationSession reusable = ConversationSession.builder().id(1L).phoneNumber(phoneNumber).build();
        when(properties.getConversation()).thenReturn(conversation);
        when(properties.getConversationSessionReuseHours()).thenReturn(null);
        when(repository.findLatestReusableByPhoneNumber(eq(phoneNumber), any(), any(), any()))
                .thenReturn(Optional.of(reusable));

        ConversationSession result = service.getOrCreate(phoneNumber);

        ArgumentCaptor<LocalDateTime> collectingAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> readyForHumanAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> humanHandoffAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findLatestReusableByPhoneNumber(
                eq(phoneNumber),
                collectingAfterCaptor.capture(),
                readyForHumanAfterCaptor.capture(),
                humanHandoffAfterCaptor.capture()
        );

        LocalDateTime collectingAfter = collectingAfterCaptor.getValue();
        LocalDateTime readyForHumanAfter = readyForHumanAfterCaptor.getValue();
        LocalDateTime humanHandoffAfter = humanHandoffAfterCaptor.getValue();

        assertThat(result).isSameAs(reusable);
        assertThat(Duration.between(readyForHumanAfter, collectingAfter).toHours()).isBetween(143L, 145L);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(readyForHumanAfter, humanHandoffAfter))).isLessThanOrEqualTo(1L);
    }

    @Test
    void fallsBackToLegacyWindowWhenStateSpecificValuesAreMissing() {
        String phoneNumber = "5491112345678";
        BeautyBotProperties.Conversation conversation = new BeautyBotProperties.Conversation();

        when(properties.getConversation()).thenReturn(conversation);
        when(properties.getConversationSessionReuseHours()).thenReturn(48);
        when(repository.findLatestReusableByPhoneNumber(eq(phoneNumber), any(), any(), any()))
                .thenReturn(Optional.of(ConversationSession.builder().id(2L).phoneNumber(phoneNumber).build()));

        service.getOrCreate(phoneNumber);

        ArgumentCaptor<LocalDateTime> collectingAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> readyForHumanAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> humanHandoffAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findLatestReusableByPhoneNumber(
                eq(phoneNumber),
                collectingAfterCaptor.capture(),
                readyForHumanAfterCaptor.capture(),
                humanHandoffAfterCaptor.capture()
        );

        LocalDateTime collectingAfter = collectingAfterCaptor.getValue();
        LocalDateTime readyForHumanAfter = readyForHumanAfterCaptor.getValue();
        LocalDateTime humanHandoffAfter = humanHandoffAfterCaptor.getValue();

        assertThat(Math.abs(ChronoUnit.SECONDS.between(collectingAfter, readyForHumanAfter))).isLessThanOrEqualTo(1L);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(readyForHumanAfter, humanHandoffAfter))).isLessThanOrEqualTo(1L);
    }

    @Test
    void usesDefaultWindowsWhenNothingIsConfigured() {
        String phoneNumber = "5491112345678";
        BeautyBotProperties.Conversation conversation = new BeautyBotProperties.Conversation();

        when(properties.getConversation()).thenReturn(conversation);
        when(properties.getConversationSessionReuseHours()).thenReturn(null);
        when(repository.findLatestReusableByPhoneNumber(eq(phoneNumber), any(), any(), any()))
                .thenReturn(Optional.of(ConversationSession.builder().id(3L).phoneNumber(phoneNumber).build()));

        service.getOrCreate(phoneNumber);

        ArgumentCaptor<LocalDateTime> collectingAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> readyForHumanAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> humanHandoffAfterCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findLatestReusableByPhoneNumber(
                eq(phoneNumber),
                collectingAfterCaptor.capture(),
                readyForHumanAfterCaptor.capture(),
                humanHandoffAfterCaptor.capture()
        );

        LocalDateTime collectingAfter = collectingAfterCaptor.getValue();
        LocalDateTime readyForHumanAfter = readyForHumanAfterCaptor.getValue();
        LocalDateTime humanHandoffAfter = humanHandoffAfterCaptor.getValue();

        assertThat(Duration.between(readyForHumanAfter, collectingAfter).toHours()).isBetween(143L, 145L);
        assertThat(Math.abs(ChronoUnit.SECONDS.between(readyForHumanAfter, humanHandoffAfter))).isLessThanOrEqualTo(1L);
    }

    @Test
    void markHumanTakeoverSetsHumanStateAndClearsWaitingField() {
        ConversationSession session = ConversationSession.builder()
                .id(10L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .waitingForField(RequiredField.NAME)
                .requiresHuman(false)
                .build();

        service.markHumanTakeover(session);

        ArgumentCaptor<ConversationSession> captor = ArgumentCaptor.forClass(ConversationSession.class);
        verify(repository, times(1)).save(captor.capture());
        ConversationSession saved = captor.getValue();

        assertThat(saved.getState()).isEqualTo(ConversationState.HUMAN_HANDOFF);
        assertThat(saved.getRequiresHuman()).isTrue();
        assertThat(saved.getWaitingForField()).isNull();
        assertThat(saved.getHumanNotifiedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void releaseHumanTakeoverReturnsSessionToCollectingData() {
        ConversationSession session = ConversationSession.builder()
                .id(11L)
                .phoneNumber("5491112345678")
                .state(ConversationState.HUMAN_HANDOFF)
                .waitingForField(RequiredField.PREFERRED_TIME)
                .requiresHuman(true)
                .build();

        service.releaseHumanTakeover(session);

        ArgumentCaptor<ConversationSession> captor = ArgumentCaptor.forClass(ConversationSession.class);
        verify(repository, times(1)).save(captor.capture());
        ConversationSession saved = captor.getValue();

        assertThat(saved.getState()).isEqualTo(ConversationState.COLLECTING_DATA);
        assertThat(saved.getRequiresHuman()).isFalse();
        assertThat(saved.getWaitingForField()).isNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
