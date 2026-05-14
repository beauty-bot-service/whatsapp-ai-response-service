package com.beautybot.whatsappairesponseservice.outbound.service.impl;

import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.beautybot.whatsappairesponseservice.observability.PhoneNumberMasker;
import com.beautybot.whatsappairesponseservice.outbound.model.OutboundMessage;
import com.beautybot.whatsappairesponseservice.outbound.state.OutboundMessageStatus;
import com.beautybot.whatsappairesponseservice.persistence.repository.OutboundMessageModelRepository;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppCloudApiClient;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OutboundMessageServiceImplTest {

    @Mock
    private OutboundMessageModelRepository repository;
    @Mock
    private WhatsAppCloudApiClient whatsAppCloudApiClient;
    @Mock
    private BeautyBotMetrics metrics;
    @Mock
    private PhoneNumberMasker phoneNumberMasker;

    private OutboundMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OutboundMessageServiceImpl(
                repository,
                whatsAppCloudApiClient,
                metrics,
                phoneNumberMasker
        );

        lenient().when(repository.save(any())).thenAnswer(invocation -> {
            OutboundMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(1L);
            }
            return message;
        });
    }

    @Test
    void sendBotReplySuccessMarksMessageAsSent() {
        ConversationSession session = session();
        when(whatsAppCloudApiClient.sendTextMessage(session.getPhoneNumber(), "hola")).thenReturn(WhatsAppSendResult.SENT);

        OutboundMessage saved = service.sendBotReply(session, "hola");

        assertThat(saved.getStatus()).isEqualTo(OutboundMessageStatus.SENT);
        assertThat(saved.getAttemptCount()).isEqualTo(1);
        assertThat(saved.getLastError()).isNull();
        assertThat(saved.getSentAt()).isNotNull();
        verify(repository, times(2)).save(any());
    }

    @Test
    void sendBotReplyFailureMarksMessageAsFailedAndStoresError() {
        ConversationSession session = session();
        when(whatsAppCloudApiClient.sendTextMessage(session.getPhoneNumber(), "hola")).thenReturn(WhatsAppSendResult.TIMEOUT);

        OutboundMessage saved = service.sendBotReply(session, "hola");

        assertThat(saved.getStatus()).isEqualTo(OutboundMessageStatus.FAILED);
        assertThat(saved.getAttemptCount()).isEqualTo(1);
        assertThat(saved.getLastError()).isEqualTo("TIMEOUT");
        assertThat(saved.getSentAt()).isNull();

        ArgumentCaptor<OutboundMessage> captor = ArgumentCaptor.forClass(OutboundMessage.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getFirst().getStatus()).isEqualTo(OutboundMessageStatus.FAILED);
    }

    @Test
    void sendBotReplyExceptionFromClientMarksMessageAsFailed() {
        ConversationSession session = session();
        when(whatsAppCloudApiClient.sendTextMessage(session.getPhoneNumber(), "hola"))
                .thenThrow(new RuntimeException("whatsapp timeout"));

        OutboundMessage saved = service.sendBotReply(session, "hola");

        assertThat(saved.getStatus()).isEqualTo(OutboundMessageStatus.FAILED);
        assertThat(saved.getLastError()).contains("whatsapp timeout");
    }

    @Test
    void sendBotReplyValidatesRequiredData() {
        assertThatThrownBy(() -> service.sendBotReply(null, "hola"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("session with id is required");

        assertThatThrownBy(() -> service.sendBotReply(ConversationSession.builder().id(1L).phoneNumber(" ").build(), "hola"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("session.phoneNumber is required");

        assertThatThrownBy(() -> service.sendBotReply(ConversationSession.builder().id(1L).phoneNumber("5491112345678").build(), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content is required");
    }

    private ConversationSession session() {
        return ConversationSession.builder()
                .id(10L)
                .phoneNumber("5491112345678")
                .build();
    }
}
