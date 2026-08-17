package com.beautybot.whatsappairesponseservice.application.promotion;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.reply.LeadCollectionReplyFactory;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import com.beautybot.whatsappairesponseservice.promotion.PromotionCatalog;
import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.PromotionDeliveryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                .reply("Te derivo con una profesional.")
                .build();

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo("Te derivo con una profesional.");
        verifyNoInteractions(promotionCatalog, promotionDeliveryRegistry, leadCollectionReplyFactory);
    }

    @Test
    void keepsNormalReplyWhenPromotionWasAlreadyDelivered() {
        ConversationContext context = context(ConversationSession.builder().id(7L).build(), "Cual es el precio del botox?");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.PRICE_QUESTION))
                .reply("Una asesora puede confirmarte el precio actualizado.")
                .shouldBotReply(true)
                .nextState(ConversationState.COLLECTING_DATA)
                .build();
        List<PromotionContent> match = List.of(
                new PromotionContent(1L, "botox", "Botox", "PROMO BOTOX")
        );
        when(promotionCatalog.match(1L, "Cual es el precio del botox?")).thenReturn(match);
        when(promotionDeliveryRegistry.filterUndelivered(7L, match)).thenReturn(List.of());

        ConversationDecision result = policy.enrich(1L, context, decision);

        assertThat(result.getReply()).isEqualTo("Una asesora puede confirmarte el precio actualizado.");
        assertThat(result.getMatchedPromotionCodes()).isEmpty();
        verify(promotionDeliveryRegistry, never()).recordDelivered(anyLong(), anyList());
    }

    @Test
    void sendsOnlyPromotionsNotPreviouslyDelivered() {
        ConversationContext context = context(ConversationSession.builder().id(9L).build(), "Botox y rinomodelado");
        ConversationDecision decision = ConversationDecision.builder()
                .intents(List.of(Intent.TREATMENT_INFO))
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

    private ConversationContext context(ConversationSession session, String message) {
        return ConversationContext.builder()
                .currentSession(session)
                .currentMessage(ChatMessage.builder().message(message).build())
                .build();
    }
}
