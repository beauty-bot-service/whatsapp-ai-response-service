package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.application.exception.AppException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationDatabaseLockService;
import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationProcessingLockService;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.service.ConversationService;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationControlServiceTest {

    @Mock
    private ConversationService conversationService;
    @Mock
    private ConversationMessageHistoryService messageHistoryService;
    @Mock
    private ConversationDatabaseLockService conversationDatabaseLockService;
    @Mock
    private ConversationProcessingLockService conversationProcessingLockService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUpTemplates() {
        lenient().when(conversationProcessingLockService.executeLocked(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Object> supplier = (Supplier<Object>) invocation.getArgument(1);
                    return supplier.get();
                });
        lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
    }

    private ConversationControlService service() {
        return new ConversationControlService(
                conversationService,
                messageHistoryService,
                conversationDatabaseLockService,
                conversationProcessingLockService,
                transactionTemplate
        );
    }

    @Test
    void takeoverByHumanMarksSessionAndStoresHumanMessageWhenPresent() {
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .build();
        when(conversationService.getOrCreate("5491112345678")).thenReturn(session);

        ConversationSession result = service().takeoverByHuman("5491112345678", "Te atiende recepción");

        assertThat(result).isSameAs(session);
        verify(conversationDatabaseLockService).lockPhoneNumber("5491112345678");
        verify(conversationService).markHumanTakeover(session);
        verify(messageHistoryService).saveHumanOutbound(session, "Te atiende recepción");
    }

    @Test
    void releaseToBotMarksSessionAsCollecting() {
        ConversationSession session = ConversationSession.builder()
                .id(2L)
                .phoneNumber("5491112345678")
                .state(ConversationState.HUMAN_HANDOFF)
                .build();
        when(conversationService.getOrCreate("5491112345678")).thenReturn(session);

        ConversationSession result = service().releaseToBot("5491112345678");

        assertThat(result).isSameAs(session);
        verify(conversationDatabaseLockService).lockPhoneNumber("5491112345678");
        verify(conversationService).releaseHumanTakeover(session);
        verify(messageHistoryService, never()).saveHumanOutbound(any(), anyString());
    }

    @Test
    void takeoverRequiresPhoneNumber() {
        assertThatThrownBy(() -> service().takeoverByHuman("  ", "hola"))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.CONVERSATION_PHONE_REQUIRED));
    }
}
