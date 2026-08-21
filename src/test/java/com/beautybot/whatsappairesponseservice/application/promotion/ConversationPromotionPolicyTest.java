package com.beautybot.whatsappairesponseservice.application.promotion;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.DecisionSource;
import com.beautybot.whatsappairesponseservice.conversation.decision.RecentConversationMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.reply.LeadCollectionReplyFactory;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.MessageDirection;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import com.beautybot.whatsappairesponseservice.promotion.PromotionCatalog;
import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.PromotionDeliveryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class ConversationPromotionPolicyTest {

    @Mock
    private PromotionCatalog promotionCatalog;
    @Mock
    private PromotionDeliveryRegistry promotionDeliveryRegistry;
    @Mock
    private LeadCollectionReplyFactory leadCollectionReplyFactory;

    private ConversationPromotionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ConversationPromotionPolicy(
                promotionCatalog,
                promotionDeliveryRegistry,
                leadCollectionReplyFactory
        );
        lenient().when(promotionDeliveryRegistry.filterUndelivered(anyLong(), anyList()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void composesMultipleCanonicalBodiesAndCollectionQuestion() {
        ConversationSession session = ConversationSession.builder().id(1L).build();
        ConversationContext context = context(session, "Quiero botox y rinomodelado");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.PRICE_QUESTION))
                .source(DecisionSource.RULE_BASED)
                .nextState(ConversationState.COLLECTING_DATA)
                .nextWaitingForField(RequiredField.NAME)
                .shouldBotReply(true)
                .reply("Una asesora valida las promociones.")
                .build();
        when(promotionCatalog.match(1L, "Quiero botox y rinomodelado")).thenReturn(List.of(
                new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX"),
                new PromotionContent(2L, "rinomodelado", "Rinomodelado", "PROMO RINOMODELADO")
        ));
        when(leadCollectionReplyFactory.askFor(RequiredField.NAME, session)).thenReturn("Me pasas tu nombre?");

        ConversationDecision enriched = policy.enrich(1L, context, decision);

        assertThat(enriched.getReply()).isEqualTo(
                "PROMO BOTOX\n\nPROMO RINOMODELADO\n\nMe pasas tu nombre?"
        );
        assertThat(enriched.getMatchedPromotionCodes()).containsExactly("botox", "rinomodelado");
        assertThat(enriched.getIntents()).contains(Intent.PRICE_QUESTION, Intent.TREATMENT_INFO);
        verify(promotionDeliveryRegistry).recordDelivered(1L, List.of(
                new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX"),
                new PromotionContent(2L, "rinomodelado", "Rinomodelado", "PROMO RINOMODELADO")
        ));
    }

    @Test
    void usesAiSelectedCodesBeforeTextMatcher() {
        ConversationContext context = context(ConversationSession.builder().id(1L).build(), "Lo que vi en redes");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.PRICE_QUESTION))
                .source(DecisionSource.AI)
                .matchedPromotionCodes(List.of("botox"))
                .nextState(ConversationState.COLLECTING_DATA)
                .shouldBotReply(true)
                .build();
        when(promotionCatalog.findActiveByCodes(1L, List.of("botox")))
                .thenReturn(List.of(new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX")));

        policy.enrich(1L, context, decision);

        verify(promotionCatalog, never()).match(1L, "Lo que vi en redes");
        assertThat(decision.getReply()).isEqualTo("PROMO BOTOX");
    }

    @Test
    void doesNotOfferPromotionDuringMedicalQuestion() {
        ConversationContext context = context(ConversationSession.builder().id(1L).build(), "Botox tiene riesgos?");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.MEDICAL_QUESTION))
                .source(DecisionSource.AI)
                .reply("Te derivo con una profesional.")
                .build();

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo("Te derivo con una profesional.");
        verifyNoInteractions(promotionCatalog, promotionDeliveryRegistry, leadCollectionReplyFactory);
    }

    @Test
    void keepsNormalReplyWhenPromotionWasAlreadyDelivered() {
        ConversationContext context = context(ConversationSession.builder().id(7L).build(), "Quiero mas informacion sobre botox");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .source(DecisionSource.AI)
                .matchedPromotionCodes(List.of("botox"))
                .reply("El tratamiento se realiza luego de una evaluacion profesional.")
                .shouldBotReply(true)
                .nextState(ConversationState.COLLECTING_DATA)
                .build();
        List<PromotionContent> match = List.of(
                new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX")
        );
        when(promotionCatalog.findActiveByCodes(1L, List.of("botox"))).thenReturn(match);
        when(promotionDeliveryRegistry.filterUndelivered(7L, match)).thenReturn(List.of());

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo(
                "El tratamiento se realiza luego de una evaluacion profesional."
        );
        assertThat(result.getMatchedPromotionCodes()).isEmpty();
        verify(promotionCatalog, never()).match(anyLong(), anyString());
        verify(promotionDeliveryRegistry, never()).recordDelivered(anyLong(), anyList());
    }

    @Test
    void sendsAllPromotionsSelectedByAiForEarlyCommercialInterest() {
        ConversationSession session = ConversationSession.builder().id(8L).build();
        ConversationContext context = context(session, "Quiero averiguar por botox e hilos");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .source(DecisionSource.AI)
                .matchedPromotionCodes(List.of("botox", "hilos"))
                .nextState(ConversationState.COLLECTING_DATA)
                .nextWaitingForField(RequiredField.NAME)
                .shouldBotReply(true)
                .build();
        List<PromotionContent> promotions = List.of(
                new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX"),
                new PromotionContent(2L, "hilos", "Hilos", "PROMO HILOS")
        );
        when(promotionCatalog.findActiveByCodes(1L, List.of("botox", "hilos"))).thenReturn(promotions);
        when(leadCollectionReplyFactory.askFor(RequiredField.NAME, session)).thenReturn("Me pasas tu nombre?");

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo(
                "PROMO BOTOX\n\nPROMO HILOS\n\nMe pasas tu nombre?"
        );
        assertThat(result.getMatchedPromotionCodes()).containsExactly("botox", "hilos");
        verify(promotionDeliveryRegistry).recordDelivered(8L, promotions);
        verify(promotionCatalog, never()).match(anyLong(), anyString());
    }

    @Test
    void sendsOnlyPromotionsNotPreviouslyDelivered() {
        ConversationContext context = context(ConversationSession.builder().id(9L).build(), "Botox y rinomodelado");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.PRICE_QUESTION))
                .source(DecisionSource.RULE_BASED)
                .shouldBotReply(true)
                .nextState(ConversationState.COLLECTING_DATA)
                .build();
        PromotionContent botox = new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX");
        PromotionContent rinomodelado = new PromotionContent(2L, "rinomodelado", "Rinomodelado", "PROMO RINO");
        List<PromotionContent> matches = List.of(botox, rinomodelado);
        when(promotionCatalog.match(1L, "Botox y rinomodelado")).thenReturn(matches);
        when(promotionDeliveryRegistry.filterUndelivered(9L, matches)).thenReturn(List.of(rinomodelado));

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo("PROMO RINO");
        assertThat(result.getMatchedPromotionCodes()).containsExactly("rinomodelado");
        verify(promotionDeliveryRegistry).recordDelivered(9L, List.of(rinomodelado));
    }

    @Test
    void doesNotMatchPromotionByTreatmentNameForAiInformationQuestion() {
        ConversationContext context = context(
                ConversationSession.builder().id(12L).build(),
                "En que consiste el botox y cuanto demora?"
        );
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .source(DecisionSource.AI)
                .reply("Es un procedimiento breve que se evalua con el profesional.")
                .shouldBotReply(true)
                .nextState(ConversationState.COLLECTING_DATA)
                .build();

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo(
                "Es un procedimiento breve que se evalua con el profesional."
        );
        assertThat(result.getMatchedPromotionCodes()).isEmpty();
        verify(promotionCatalog, never()).match(anyLong(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Quiero información sobre botox",
            "Quiero saber más de botox",
            "Me interesa botox",
            "Lo vi en Instagram, quiero botox"
    })
    void matchesPromotionForGeneralInterestDuringFirstCustomerMessages(String message) {
        ConversationContext context = context(
                ConversationSession.builder().id(13L).build(),
                message,
                List.of(inbound(message))
        );
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .source(DecisionSource.AI)
                .reply("Te cuento sobre el tratamiento.")
                .shouldBotReply(true)
                .nextState(ConversationState.COLLECTING_DATA)
                .build();
        PromotionContent promotion = new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX");
        when(promotionCatalog.match(1L, message)).thenReturn(List.of(promotion));

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo("PROMO BOTOX");
        assertThat(result.getMatchedPromotionCodes()).containsExactly("botox");
    }

    @Test
    void proceduralQuestionOverridesGeneralInterestPhrase() {
        String message = "Quiero saber mas: en que consiste el botox?";
        ConversationContext context = context(
                ConversationSession.builder().id(14L).build(),
                message,
                List.of(inbound(message))
        );
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .source(DecisionSource.AI)
                .reply("El procedimiento se explica de forma general.")
                .shouldBotReply(true)
                .nextState(ConversationState.COLLECTING_DATA)
                .build();

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo("El procedimiento se explica de forma general.");
        verify(promotionCatalog, never()).match(anyLong(), anyString());
    }

    @Test
    void doesNotApplyGeneralInterestFallbackAfterSecondCustomerMessage() {
        String message = "Quiero informacion sobre botox";
        ConversationContext context = context(
                ConversationSession.builder().id(15L).build(),
                message,
                List.of(inbound("Hola"), inbound("Quiero consultar"), inbound(message))
        );
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
                .source(DecisionSource.AI)
                .reply("Te cuento sobre el tratamiento.")
                .shouldBotReply(true)
                .nextState(ConversationState.COLLECTING_DATA)
                .build();

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo("Te cuento sobre el tratamiento.");
        verify(promotionCatalog, never()).match(anyLong(), anyString());
    }

    private ConversationContext context(ConversationSession session, String message) {
        return context(session, message, null);
    }

    private ConversationContext context(
            ConversationSession session,
            String message,
            List<RecentConversationMessage> recentMessages
    ) {
        return ConversationContext.builder()
                .currentSession(session)
                .currentMessage(ChatMessage.builder().message(message).build())
                .recentMessages(recentMessages)
                .build();
    }

    private RecentConversationMessage inbound(String content) {
        return RecentConversationMessage.builder()
                .direction(MessageDirection.INBOUND)
                .content(content)
                .build();
    }
}
