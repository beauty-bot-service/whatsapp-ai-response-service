package com.beautybot.whatsappairesponseservice.ai;

import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedMessageAnalyzerTest {

    private final RuleBasedMessageAnalyzer analyzer = new RuleBasedMessageAnalyzer();

    @Test
    void detectsMultipleIntentsInOneMessage() {
        ConversationSession session = ConversationSession.builder().build();

        MessageAnalysis analysis = analyzer.analyze("Hola, me pasas ubicacion y horarios?", session);

        assertThat(analysis.getIntents())
                .contains(Intent.GREETING, Intent.LOCATION_QUESTION, Intent.OPENING_HOURS_QUESTION);
        assertThat(analysis.getIntent()).isEqualTo(Intent.LOCATION_QUESTION);
    }

    @Test
    void doesNotAssumeFirstTimeFromGenericYesWhenNotWaitingForThatField() {
        ConversationSession session = ConversationSession.builder().build();

        MessageAnalysis analysis = analyzer.analyze("si, quiero saber el precio", session);

        assertThat(analysis.getFirstTime()).isNull();
        assertThat(analysis.getIntents()).contains(Intent.PRICE_QUESTION);
    }

    @Test
    void resolvesFirstTimeFromYesWhenSessionIsWaitingForFirstTime() {
        ConversationSession session = ConversationSession.builder()
                .waitingForField(RequiredField.FIRST_TIME)
                .build();

        MessageAnalysis analysis = analyzer.analyze("si", session);

        assertThat(analysis.getFirstTime()).isTrue();
    }

    @Test
    void doesNotCapturePlainNumbersAsPreferredTimeWhenNotWaitingForTime() {
        ConversationSession session = ConversationSession.builder().build();

        MessageAnalysis analysis = analyzer.analyze("quiero 2 zonas", session);

        assertThat(analysis.getPreferredTime()).isNull();
    }

    @Test
    void extractsNameConservativelyFromSelfIdentificationWithAdditionalIntent() {
        ConversationSession session = ConversationSession.builder().build();

        MessageAnalysis analysis = analyzer.analyze("Soy Gian y quiero botox", session);

        assertThat(analysis.getExtractedName()).isEqualTo("Gian");
        assertThat(analysis.getTreatment()).isEqualTo("Botox");
    }

    @Test
    void trimsTrailingContextWhenWaitingForName() {
        ConversationSession session = ConversationSession.builder()
                .waitingForField(RequiredField.NAME)
                .build();

        MessageAnalysis analysis = analyzer.analyze("Gian y quiero botox", session);

        assertThat(analysis.getExtractedName()).isEqualTo("Gian");
    }

    @Test
    void detectsAvailabilityIntent() {
        ConversationSession session = ConversationSession.builder().build();

        MessageAnalysis analysis = analyzer.analyze("Hola, tienen disponibilidad para esta semana?", session);

        assertThat(analysis.getIntents()).contains(Intent.AVAILABILITY_QUESTION);
    }

    @Test
    void detectsAvailabilityIntentWhenDateIsProvided() {
        ConversationSession session = ConversationSession.builder().build();

        MessageAnalysis analysis = analyzer.analyze("Tienen turno para el 5/4?", session);

        assertThat(analysis.getIntents()).contains(Intent.AVAILABILITY_QUESTION);
    }

    @Test
    void extractsPreferredTimeWhenWaitingAndMessageContainsDateAndTime() {
        ConversationSession session = ConversationSession.builder()
                .waitingForField(RequiredField.PREFERRED_TIME)
                .build();

        MessageAnalysis analysis = analyzer.analyze("5/4 a las 15:30", session);

        assertThat(analysis.getPreferredTime()).isEqualTo("5/4 a las 15:30");
    }
}
