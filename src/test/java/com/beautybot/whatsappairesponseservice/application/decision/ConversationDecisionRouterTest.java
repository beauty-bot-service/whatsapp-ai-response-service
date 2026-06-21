package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ConversationDecisionRouterTest {

    @Test
    void routesToHumanWhenAvailabilityLookupFailed() {
        AiConversationDecisionService aiService = mock(AiConversationDecisionService.class);
        RuleBasedConversationDecisionService ruleBasedService = mock(RuleBasedConversationDecisionService.class);
        ConversationDecisionRouter router = new ConversationDecisionRouter(
                aiService,
                ruleBasedService,
                new ConversationDecisionValidator(),
                new BeautyBotProperties(),
                mock(BeautyBotMetrics.class)
        );
        ConversationContext context = ConversationContext.builder()
                .currentSession(ConversationSession.builder()
                        .id(1L)
                        .phoneNumber("5491112345678")
                        .state(ConversationState.COLLECTING_DATA)
                        .build())
                .availabilityLookupFailed(true)
                .availabilityFailureReason("freeBusy failed")
                .build();

        ConversationDecision decision = router.decide(context);

        assertThat(decision.getNextState()).isEqualTo(ConversationState.HUMAN_HANDOFF);
        assertThat(decision.getRequiresHuman()).isTrue();
        assertThat(decision.getShouldNotifyHuman()).isTrue();
        assertThat(decision.getShouldBotReply()).isTrue();
        assertThat(decision.getReply()).contains("Derivo tu consulta");
        verifyNoInteractions(aiService, ruleBasedService);
    }
}
