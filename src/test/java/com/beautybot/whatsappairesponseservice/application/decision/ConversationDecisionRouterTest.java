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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationDecisionRouterTest {

    @Test
    void ignoresLegacyCalendarFailureAndUsesConversationEngine() {
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
        ConversationDecision ruleBasedDecision = ConversationDecision.builder()
                .nextState(ConversationState.COLLECTING_DATA)
                .shouldBotReply(true)
                .reply("Indica la fecha que preferis")
                .build();
        when(ruleBasedService.decide(context)).thenReturn(ruleBasedDecision);

        ConversationDecision decision = router.decide(context);

        assertThat(decision.getNextState()).isEqualTo(ConversationState.COLLECTING_DATA);
        assertThat(decision.getShouldBotReply()).isTrue();
        verify(ruleBasedService).decide(context);
    }
}
