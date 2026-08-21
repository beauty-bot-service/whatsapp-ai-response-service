package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.ExtractedConversationData;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void allowsBasicMedicalInformationWhenNoSensitiveSignalIsPresent() {
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .build();
        ConversationContext context = ConversationContext.builder()
                .currentSession(session)
                .currentMessage(ChatMessage.builder()
                        .message("En que consiste el tratamiento?")
                        .build())
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.MEDICAL_QUESTION))
                .nextState(ConversationState.COLLECTING_DATA)
                .extractedData(ExtractedConversationData.builder().build())
                .requiresHuman(false)
                .shouldNotifyHuman(false)
                .shouldBotReply(true)
                .reply("Es una explicacion general breve. Te interesa coordinar? 😊")
                .build();

        ConversationDecision validated = validator.validateAndFix(decision, context);

        assertThat(validated.getNextState()).isEqualTo(ConversationState.COLLECTING_DATA);
        assertThat(validated.getRequiresHuman()).isFalse();
        assertThat(validated.getShouldNotifyHuman()).isFalse();
        assertThat(validated.getReply()).contains("explicacion general").contains("😊");
    }

    @Test
    void allowsGeneralTreatmentInformationWithoutForcingHandoff() {
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .build();
        ConversationContext context = ConversationContext.builder()
                .currentSession(session)
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .nextState(ConversationState.COLLECTING_DATA)
                .nextWaitingForField(com.beautybot.whatsappairesponseservice.conversation.state.RequiredField.NAME)
                .extractedData(ExtractedConversationData.builder()
                        .treatmentInterest("Botox")
                        .build())
                .shouldBotReply(true)
                .reply("Es una explicacion general breve. Como te llamas?")
                .build();

        ConversationDecision validated = validator.validateAndFix(decision, context);

        assertThat(validated.getNextState()).isEqualTo(ConversationState.COLLECTING_DATA);
        assertThat(validated.getRequiresHuman()).isFalse();
        assertThat(validated.getReply()).contains("explicacion general");
    }

    @Test
    void forcesHandoffFromMessageContentWhenAiMisclassifiesRiskQuestion() {
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .build();
        ConversationContext context = ConversationContext.builder()
                .currentSession(session)
                .currentMessage(ChatMessage.builder()
                        .message("Que riesgos tiene si estoy embarazada?")
                        .build())
                .build();
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .nextState(ConversationState.COLLECTING_DATA)
                .extractedData(ExtractedConversationData.builder().build())
                .shouldBotReply(true)
                .reply("La IA intento responder")
                .build();

        ConversationDecision validated = validator.validateAndFix(decision, context);

        assertThat(validated.getIntents()).startsWith(Intent.MEDICAL_QUESTION);
        assertThat(validated.getNextState()).isEqualTo(ConversationState.HUMAN_HANDOFF);
        assertThat(validated.getReply()).doesNotContain("intento responder");
    }
}
