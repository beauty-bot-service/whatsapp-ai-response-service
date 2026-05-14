package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationService;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.outbound.notification.HumanNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HumanHandoffService {

    private enum HumanWaitingState {
        NONE,
        READY_FOR_HUMAN,
        HUMAN_HANDOFF
    }

    private final HumanNotificationService humanNotificationService;
    private final ConversationService conversationService;

    public boolean isWaitingForHuman(ConversationSession session) {
        return resolveHumanWaitingState(session.getState()) != HumanWaitingState.NONE;
    }

    public boolean isReadyForHuman(ConversationSession session) {
        return resolveHumanWaitingState(session.getState()) == HumanWaitingState.READY_FOR_HUMAN;
    }

    public boolean isHumanHandoff(ConversationSession session) {
        return resolveHumanWaitingState(session.getState()) == HumanWaitingState.HUMAN_HANDOFF;
    }

    public void notifyAdvisorAboutNewCustomerMessage(ConversationSession session, String message) {
        humanNotificationService.notifyAdvisorAboutNewCustomerMessage(session, message);
    }

    public void notifyAdvisorIfTransitioned(
            ConversationState previousState,
            ConversationDecision finalDecision,
            ConversationSession session
    ) {
        if (!shouldNotifyAdvisor(previousState, finalDecision, session)) {
            return;
        }
        humanNotificationService.notifyAdvisor(session);
        conversationService.markHumanNotifiedNow(session);
    }

    private boolean shouldNotifyAdvisor(
            ConversationState previousState,
            ConversationDecision finalDecision,
            ConversationSession session
    ) {
        if (!Boolean.TRUE.equals(finalDecision.getRequiresHuman())) {
            return false;
        }
        if (resolveHumanWaitingState(finalDecision.getNextState()) == HumanWaitingState.NONE) {
            return false;
        }
        if (resolveHumanWaitingState(previousState) != HumanWaitingState.NONE) {
            return false;
        }
        return session.getHumanNotifiedAt() == null;
    }

    private HumanWaitingState resolveHumanWaitingState(ConversationState state) {
        if (state == ConversationState.READY_FOR_HUMAN) {
            return HumanWaitingState.READY_FOR_HUMAN;
        }
        if (state == ConversationState.HUMAN_HANDOFF) {
            return HumanWaitingState.HUMAN_HANDOFF;
        }
        return HumanWaitingState.NONE;
    }
}
