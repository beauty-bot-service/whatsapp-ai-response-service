package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationService;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.outbound.notification.HumanNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HumanHandoffServiceTest {

    @Mock
    private HumanNotificationService humanNotificationService;
    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private HumanHandoffService humanHandoffService;

    @Test
    void differentiatesReadyForHumanAndHumanHandoffStates() {
        ConversationSession ready = ConversationSession.builder()
                .state(ConversationState.READY_FOR_HUMAN)
                .build();
        ConversationSession handoff = ConversationSession.builder()
                .state(ConversationState.HUMAN_HANDOFF)
                .build();
        ConversationSession collecting = ConversationSession.builder()
                .state(ConversationState.COLLECTING_DATA)
                .build();

        assertThat(humanHandoffService.isWaitingForHuman(ready)).isTrue();
        assertThat(humanHandoffService.isReadyForHuman(ready)).isTrue();
        assertThat(humanHandoffService.isHumanHandoff(ready)).isFalse();

        assertThat(humanHandoffService.isWaitingForHuman(handoff)).isTrue();
        assertThat(humanHandoffService.isReadyForHuman(handoff)).isFalse();
        assertThat(humanHandoffService.isHumanHandoff(handoff)).isTrue();

        assertThat(humanHandoffService.isWaitingForHuman(collecting)).isFalse();
        assertThat(humanHandoffService.isReadyForHuman(collecting)).isFalse();
        assertThat(humanHandoffService.isHumanHandoff(collecting)).isFalse();
    }

    @Test
    void notifiesAdvisorWhenTransitioningIntoReadyForHuman() {
        ConversationSession session = ConversationSession.builder()
                .state(ConversationState.COLLECTING_DATA)
                .humanNotifiedAt(null)
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .nextState(ConversationState.READY_FOR_HUMAN)
                .requiresHuman(true)
                .build();

        humanHandoffService.notifyAdvisorIfTransitioned(ConversationState.COLLECTING_DATA, decision, session);

        verify(humanNotificationService).notifyAdvisor(session);
        verify(conversationService).markHumanNotifiedNow(session);
    }

    @Test
    void notifiesAdvisorWhenTransitioningIntoHumanHandoff() {
        ConversationSession session = ConversationSession.builder()
                .state(ConversationState.COLLECTING_DATA)
                .humanNotifiedAt(null)
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .nextState(ConversationState.HUMAN_HANDOFF)
                .requiresHuman(true)
                .build();

        humanHandoffService.notifyAdvisorIfTransitioned(ConversationState.COLLECTING_DATA, decision, session);

        verify(humanNotificationService).notifyAdvisor(session);
        verify(conversationService).markHumanNotifiedNow(session);
    }

    @Test
    void doesNotNotifyAdvisorWhenSessionWasAlreadyWaitingForHuman() {
        ConversationSession session = ConversationSession.builder()
                .state(ConversationState.READY_FOR_HUMAN)
                .humanNotifiedAt(null)
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .nextState(ConversationState.HUMAN_HANDOFF)
                .requiresHuman(true)
                .build();

        humanHandoffService.notifyAdvisorIfTransitioned(ConversationState.READY_FOR_HUMAN, decision, session);

        verifyNoInteractions(humanNotificationService, conversationService);
    }

    @Test
    void doesNotNotifyAdvisorWhenAlreadyNotifiedBefore() {
        ConversationSession session = ConversationSession.builder()
                .state(ConversationState.COLLECTING_DATA)
                .humanNotifiedAt(LocalDateTime.now())
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .nextState(ConversationState.READY_FOR_HUMAN)
                .requiresHuman(true)
                .build();

        humanHandoffService.notifyAdvisorIfTransitioned(ConversationState.COLLECTING_DATA, decision, session);

        verifyNoInteractions(humanNotificationService, conversationService);
    }
}
