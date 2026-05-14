package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.ExtractedConversationData;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationDecisionValidatorTest {

    private final ConversationDecisionValidator validator = new ConversationDecisionValidator();

    @Test
    void preventsReadyForHumanWhenRequiredDataIsMissing() {
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .build();
        ConversationContext context = ConversationContext.builder()
                .currentSession(session)
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .nextState(ConversationState.READY_FOR_HUMAN)
                .extractedData(ExtractedConversationData.builder().build())
                .shouldBotReply(true)
                .reply("paso con asesora")
                .build();

        ConversationDecision validated = validator.validateAndFix(decision, context);

        assertThat(validated.getNextState()).isEqualTo(ConversationState.COLLECTING_DATA);
        assertThat(validated.getRequiresHuman()).isFalse();
        assertThat(validated.getShouldNotifyHuman()).isFalse();
        assertThat(validated.getShouldBotReply()).isTrue();
        assertThat(validated.getReply()).isNotBlank();
    }

    @Test
    void forcesHumanFlagsForHumanStates() {
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .build();
        ConversationContext context = ConversationContext.builder()
                .currentSession(session)
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .nextState(ConversationState.HUMAN_HANDOFF)
                .requiresHuman(false)
                .shouldNotifyHuman(false)
                .shouldBotReply(false)
                .reply("no deberia quedar")
                .build();

        ConversationDecision validated = validator.validateAndFix(decision, context);

        assertThat(validated.getRequiresHuman()).isTrue();
        assertThat(validated.getShouldNotifyHuman()).isTrue();
        assertThat(validated.getShouldBotReply()).isFalse();
        assertThat(validated.getReply()).isNull();
    }
}
