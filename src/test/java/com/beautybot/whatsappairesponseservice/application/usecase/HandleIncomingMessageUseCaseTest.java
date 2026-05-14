package com.beautybot.whatsappairesponseservice.application.usecase;

import com.beautybot.whatsappairesponseservice.application.decision.ConversationContextBuilder;
import com.beautybot.whatsappairesponseservice.application.decision.ConversationDecisionRouter;
import com.beautybot.whatsappairesponseservice.application.decision.ConversationDecisionValidator;
import com.beautybot.whatsappairesponseservice.application.support.ChatMessageValidator;
import com.beautybot.whatsappairesponseservice.application.support.ConversationMessageHistoryService;
import com.beautybot.whatsappairesponseservice.application.support.HumanHandoffService;
import com.beautybot.whatsappairesponseservice.application.support.InboundMessageNormalizer;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.DecisionSource;
import com.beautybot.whatsappairesponseservice.conversation.decision.ExtractedConversationData;
import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationDatabaseLockService;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatResult;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationService;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.lead.service.LeadService;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleIncomingMessageUseCaseTest {

    @Mock
    private ConversationService conversationService;
    @Mock
    private ConversationMessageHistoryService messageHistoryService;
    @Mock
    private LeadService leadService;
    @Mock
    private HumanHandoffService humanHandoffService;
    @Mock
    private InboundMessageNormalizer inboundMessageNormalizer;
    @Mock
    private ChatMessageValidator chatMessageValidator;
    @Mock
    private ConversationContextBuilder conversationContextBuilder;
    @Mock
    private ConversationDecisionRouter conversationDecisionRouter;
    @Mock
    private ConversationDecisionValidator conversationDecisionValidator;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private BeautyBotMetrics metrics;
    @Mock
    private ConversationDatabaseLockService conversationDatabaseLockService;
    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpTransactionTemplate() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        lenient().doAnswer(invocation -> {
            java.util.function.Consumer<?> callback = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<Object> consumer = (java.util.function.Consumer<Object>) callback;
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private HandleIncomingMessageUseCase useCase() {
        return new HandleIncomingMessageUseCase(
                conversationService,
                messageHistoryService,
                leadService,
                humanHandoffService,
                inboundMessageNormalizer,
                chatMessageValidator,
                conversationContextBuilder,
                conversationDecisionRouter,
                conversationDecisionValidator,
                transactionTemplate,
                metrics,
                conversationDatabaseLockService,
                objectMapper
        );
    }

    @Test
    void inboundDuplicatedDoesNotLookupSessionAndDoesNotReply() {
        ChatMessage request = inboundMessage("msg-dup", "hola");

        when(inboundMessageNormalizer.normalize(request)).thenReturn(request);
        when(messageHistoryService.isInboundAlreadyProcessed(request)).thenReturn(true);

        ChatResult result = useCase().handleMessage(request);

        verify(chatMessageValidator).validateNormalized(request);
        verify(conversationService, never()).getOrCreate(anyString());
        verify(messageHistoryService, never()).saveInbound(any(), any());
        verify(leadService, never()).upsertFromConversation(any());
        verifyNoInteractions(leadService, humanHandoffService, conversationContextBuilder, conversationDecisionRouter);

        assertThat(result.getReply()).isNull();
        assertThat(result.getRequiresHuman()).isFalse();
        assertThat(result.getState()).isNull();
    }

    @Test
    void saveInboundFalseDoesNotProcessConversation() {
        ChatMessage request = inboundMessage("msg-2", "hola");
        ConversationSession session = session(ConversationState.COLLECTING_DATA, false);

        when(inboundMessageNormalizer.normalize(request)).thenReturn(request);
        when(messageHistoryService.isInboundAlreadyProcessed(request)).thenReturn(false);
        when(conversationService.getOrCreate(request.getPhoneNumber())).thenReturn(session);
        when(messageHistoryService.saveInbound(session, request)).thenReturn(false);

        ChatResult result = useCase().handleMessage(request);

        verify(chatMessageValidator).validateNormalized(request);
        verify(humanHandoffService, never()).isWaitingForHuman(any());
        verify(messageHistoryService, never()).saveOutbound(any(), anyString());
        verify(leadService, never()).upsertFromConversation(any());
        verifyNoInteractions(leadService, conversationContextBuilder, conversationDecisionRouter);

        assertThat(result.getReply()).isNull();
        assertThat(result.getState()).isEqualTo(ConversationState.COLLECTING_DATA);
        assertThat(result.getRequiresHuman()).isFalse();
    }

    @Test
    void readyForHumanDoesNotCallDecisionRouter() {
        ChatMessage request = inboundMessage("msg-3", "hola, sigo esperando");
        ConversationSession session = session(ConversationState.READY_FOR_HUMAN, true);

        when(inboundMessageNormalizer.normalize(request)).thenReturn(request);
        when(messageHistoryService.isInboundAlreadyProcessed(request)).thenReturn(false);
        when(conversationService.getOrCreate(request.getPhoneNumber())).thenReturn(session);
        when(messageHistoryService.saveInbound(session, request)).thenReturn(true);
        when(humanHandoffService.isWaitingForHuman(session)).thenReturn(true);

        ChatResult result = useCase().handleMessage(request);

        verify(chatMessageValidator).validateNormalized(request);

        InOrder inOrder = inOrder(conversationService, humanHandoffService);
        inOrder.verify(conversationService).markCustomerMessageNow(session);
        inOrder.verify(humanHandoffService).notifyAdvisorAboutNewCustomerMessage(session, request.getMessage());

        assertThat(result.getReply()).isNull();
        assertThat(result.getState()).isEqualTo(ConversationState.READY_FOR_HUMAN);
        assertThat(result.getRequiresHuman()).isTrue();

        verify(messageHistoryService, never()).saveOutbound(any(), anyString());
        verify(leadService, never()).upsertFromConversation(any());
        verifyNoInteractions(leadService, conversationContextBuilder, conversationDecisionRouter);
    }

    @Test
    void humanHandoffDoesNotCallDecisionRouter() {
        ChatMessage request = inboundMessage("msg-4", "sigo esperando");
        ConversationSession session = session(ConversationState.HUMAN_HANDOFF, true);

        when(inboundMessageNormalizer.normalize(request)).thenReturn(request);
        when(messageHistoryService.isInboundAlreadyProcessed(request)).thenReturn(false);
        when(conversationService.getOrCreate(request.getPhoneNumber())).thenReturn(session);
        when(messageHistoryService.saveInbound(session, request)).thenReturn(true);
        when(humanHandoffService.isWaitingForHuman(session)).thenReturn(true);

        ChatResult result = useCase().handleMessage(request);

        verify(chatMessageValidator).validateNormalized(request);
        verify(conversationService).markCustomerMessageNow(session);
        verify(humanHandoffService).notifyAdvisorAboutNewCustomerMessage(session, request.getMessage());
        verify(conversationDecisionRouter, never()).decide(any());
        verify(leadService, never()).upsertFromConversation(any());
        verify(messageHistoryService, never()).saveOutbound(any(), anyString());

        assertThat(result.getReply()).isNull();
        assertThat(result.getState()).isEqualTo(ConversationState.HUMAN_HANDOFF);
        assertThat(result.getRequiresHuman()).isTrue();
    }

    @Test
    void transitionToReadyForHumanCreatesLeadAndNotifiesOnce() {
        ChatMessage request = inboundMessage("msg-5", "quiero reservar un turno");
        ConversationSession session = session(ConversationState.COLLECTING_DATA, false);
        ConversationContext context = ConversationContext.builder().currentSession(session).currentMessage(request).build();
        ConversationDecision decision = readyForHumanDecision("Te paso con un asesor.");
        when(inboundMessageNormalizer.normalize(request)).thenReturn(request);
        when(messageHistoryService.isInboundAlreadyProcessed(request)).thenReturn(false);
        when(conversationService.getOrCreate(request.getPhoneNumber())).thenReturn(session);
        when(messageHistoryService.saveInbound(session, request)).thenReturn(true);
        when(humanHandoffService.isWaitingForHuman(session)).thenReturn(false);
        when(conversationContextBuilder.build(session, request)).thenReturn(context);
        when(conversationDecisionRouter.decide(context)).thenReturn(decision);

        ChatResult result = useCase().handleMessage(request);

        verify(chatMessageValidator).validateNormalized(request);
        verify(conversationDecisionRouter).decide(context);
        verify(conversationDecisionValidator).applyExtractedDataToSession(session, decision);
        verify(conversationService).applyDecision(session, decision);
        verify(humanHandoffService, times(1))
                .notifyAdvisorIfTransitioned(ConversationState.COLLECTING_DATA, decision, session);
        verify(humanHandoffService, never()).notifyAdvisorAboutNewCustomerMessage(any(), anyString());
        verify(leadService).upsertFromConversation(any());
        verify(messageHistoryService).saveOutbound(session, decision.getReply());

        assertThat(result.getReply()).isEqualTo("Te paso con un asesor.");
        assertThat(result.getState()).isEqualTo(ConversationState.READY_FOR_HUMAN);
        assertThat(result.getRequiresHuman()).isTrue();
    }

    @Test
    void messageAfterReadyForHumanDoesNotCreateAnotherLead() {
        ChatMessage request = inboundMessage("msg-6", "hola, me ayudan?");
        ConversationSession session = session(ConversationState.READY_FOR_HUMAN, true);

        when(inboundMessageNormalizer.normalize(request)).thenReturn(request);
        when(messageHistoryService.isInboundAlreadyProcessed(request)).thenReturn(false);
        when(conversationService.getOrCreate(request.getPhoneNumber())).thenReturn(session);
        when(messageHistoryService.saveInbound(session, request)).thenReturn(true);
        when(humanHandoffService.isWaitingForHuman(session)).thenReturn(true);

        useCase().handleMessage(request);

        verify(leadService, never()).upsertFromConversation(any());
    }

    @Test
    void leadUpsertFailureDoesNotBreakReplyFlow() {
        ChatMessage request = inboundMessage("msg-7", "quiero reservar");
        ConversationSession session = session(ConversationState.COLLECTING_DATA, false);
        ConversationContext context = ConversationContext.builder().currentSession(session).currentMessage(request).build();
        ConversationDecision decision = readyForHumanDecision("Te paso con un asesor.");

        when(inboundMessageNormalizer.normalize(request)).thenReturn(request);
        when(messageHistoryService.isInboundAlreadyProcessed(request)).thenReturn(false);
        when(conversationService.getOrCreate(request.getPhoneNumber())).thenReturn(session);
        when(messageHistoryService.saveInbound(session, request)).thenReturn(true);
        when(humanHandoffService.isWaitingForHuman(session)).thenReturn(false);
        when(conversationContextBuilder.build(session, request)).thenReturn(context);
        when(conversationDecisionRouter.decide(context)).thenReturn(decision);
        doThrow(new RuntimeException("db lead failure")).when(leadService).upsertFromConversation(any());

        ChatResult result = useCase().handleMessage(request);

        verify(messageHistoryService).saveOutbound(session, decision.getReply());
        assertThat(result.getReply()).isEqualTo("Te paso con un asesor.");
        assertThat(result.getState()).isEqualTo(ConversationState.READY_FOR_HUMAN);
    }

    private ConversationDecision readyForHumanDecision(String reply) {
        return ConversationDecision.builder()
                .source(DecisionSource.AI)
                .intents(List.of())
                .nextState(ConversationState.READY_FOR_HUMAN)
                .extractedData(ExtractedConversationData.builder().build())
                .requiresHuman(true)
                .shouldCreateLead(true)
                .shouldNotifyHuman(true)
                .shouldBotReply(true)
                .reply(reply)
                .build();
    }

    private ChatMessage inboundMessage(String externalMessageId, String message) {
        return ChatMessage.builder()
                .phoneNumber("5491112345678")
                .message(message)
                .channel("WHATSAPP")
                .externalMessageId(externalMessageId)
                .build();
    }

    private ConversationSession session(ConversationState state, boolean requiresHuman) {
        return ConversationSession.builder()
                .id(10L)
                .phoneNumber("5491112345678")
                .state(state)
                .requiresHuman(requiresHuman)
                .build();
    }
}
