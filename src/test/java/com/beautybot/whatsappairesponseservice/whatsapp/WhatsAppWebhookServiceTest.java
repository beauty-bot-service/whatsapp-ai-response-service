package com.beautybot.whatsappairesponseservice.whatsapp;

import com.beautybot.whatsappairesponseservice.application.ChatService;
import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationProcessingLockService;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatResult;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.beautybot.whatsappairesponseservice.observability.PhoneNumberMasker;
import com.beautybot.whatsappairesponseservice.outbound.model.OutboundMessage;
import com.beautybot.whatsappairesponseservice.outbound.state.OutboundMessageStatus;
import com.beautybot.whatsappairesponseservice.outbound.service.OutboundMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookServiceTest {

    @Mock
    private ChatService chatService;
    @Mock
    private WhatsAppWebhookParser webhookParser;
    @Mock
    private OutboundMessageService outboundMessageService;
    @Mock
    private WhatsAppWebhookSignatureValidator signatureValidator;
    @Mock
    private ConversationProcessingLockService conversationProcessingLockService;
    @Mock
    private BeautyBotMetrics metrics;
    @Mock
    private PhoneNumberMasker phoneNumberMasker;

    @Test
    void processWebhookUsesOutboundServiceToSendBotReply() {
        BeautyBotProperties properties = new BeautyBotProperties();
        properties.getWhatsapp().setEnabled(true);

        ConversationSession session = ConversationSession.builder()
                .id(10L)
                .phoneNumber("5491112345678")
                .build();
        ChatResult result = ChatResult.builder()
                .session(session)
                .reply("hola")
                .build();

        when(webhookParser.extractInboundMessages(any()))
                .thenReturn(List.of(new WhatsAppInboundMessage("wamid.1", "5491112345678", "hola")));
        when(chatService.handleMessage(any())).thenReturn(result);
        when(outboundMessageService.sendBotReply(eq(session), eq("hola")))
                .thenReturn(OutboundMessage.builder().id(1L).status(OutboundMessageStatus.SENT).build());
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(conversationProcessingLockService).executeLocked(any(), any(Runnable.class));

        WhatsAppWebhookService service = new WhatsAppWebhookService(
                properties,
                chatService,
                webhookParser,
                outboundMessageService,
                signatureValidator,
                new ObjectMapper(),
                conversationProcessingLockService,
                metrics,
                phoneNumberMasker
        );

        service.processWebhook("{}");

        verify(outboundMessageService).sendBotReply(eq(session), eq("hola"));
    }

    @Test
    void duplicateInboundDoesNotGenerateSecondOutboundReply() {
        BeautyBotProperties properties = new BeautyBotProperties();
        properties.getWhatsapp().setEnabled(true);

        ConversationSession session = ConversationSession.builder()
                .id(10L)
                .phoneNumber("5491112345678")
                .build();
        ChatResult first = ChatResult.builder()
                .session(session)
                .reply("hola")
                .build();
        ChatResult duplicate = ChatResult.builder()
                .session(session)
                .reply(null)
                .build();

        when(webhookParser.extractInboundMessages(any()))
                .thenReturn(List.of(
                        new WhatsAppInboundMessage("wamid.dup", "5491112345678", "hola"),
                        new WhatsAppInboundMessage("wamid.dup", "5491112345678", "hola")
                ));
        when(chatService.handleMessage(any())).thenReturn(first, duplicate);
        when(outboundMessageService.sendBotReply(eq(session), eq("hola")))
                .thenReturn(OutboundMessage.builder().id(1L).status(OutboundMessageStatus.SENT).build());
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(conversationProcessingLockService).executeLocked(any(), any(Runnable.class));

        WhatsAppWebhookService service = new WhatsAppWebhookService(
                properties,
                chatService,
                webhookParser,
                outboundMessageService,
                signatureValidator,
                new ObjectMapper(),
                conversationProcessingLockService,
                metrics,
                phoneNumberMasker
        );

        service.processWebhook("{}");

        verify(outboundMessageService, times(1)).sendBotReply(eq(session), eq("hola"));
    }
}
