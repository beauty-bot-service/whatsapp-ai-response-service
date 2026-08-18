package com.beautybot.whatsappairesponseservice.whatsapp;

import com.beautybot.whatsappairesponseservice.application.exception.InvalidChatMessageException;
import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.lock.ConversationProcessingLockService;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatResult;
import com.beautybot.whatsappairesponseservice.application.ChatService;
import com.beautybot.whatsappairesponseservice.outbound.service.OutboundMessageService;
import com.beautybot.whatsappairesponseservice.outbound.state.OutboundMessageStatus;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.beautybot.whatsappairesponseservice.observability.PhoneNumberMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppWebhookService {
    private static final String CHANNEL_WHATSAPP = "WHATSAPP";
    private final BeautyBotProperties properties;
    private final ChatService chatService;
    private final WhatsAppWebhookParser webhookParser;
    private final OutboundMessageService outboundMessageService;
    private final WhatsAppWebhookSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;
    private final ConversationProcessingLockService conversationProcessingLockService;
    private final BeautyBotMetrics metrics;
    private final PhoneNumberMasker phoneNumberMasker;

    public boolean isValidSignature(String rawPayload, String signatureHeader) {
        BeautyBotProperties.Whatsapp whatsapp = properties.getWhatsapp();
        if (!whatsapp.isEnabled()) {
            return true;
        }
        return signatureValidator.isValid(rawPayload, signatureHeader, whatsapp.getAppSecret());
    }

    @Async("webhookTaskExecutor")
    public void processWebhookAsync(String rawPayload) {
        try {
            processWebhook(rawPayload);
        } catch (Exception ex) {
            metrics.inboundMessage(CHANNEL_WHATSAPP, "async_failed");
            log.error("Critical: unhandled error processing WhatsApp webhook asynchronously. cause={}",
                    ex.getMessage(), ex);
        }
    }

    public int processWebhook(String rawPayload) {
        if (!properties.getWhatsapp().isEnabled()) {
            return 0;
        }

        JsonNode payload = parsePayload(rawPayload);
        if (payload == null) {
            return 0;
        }

        List<WhatsAppInboundMessage> inboundMessages = webhookParser.extractInboundMessages(payload);
        for (WhatsAppInboundMessage inbound : inboundMessages) {
            log.info("Inbound WhatsApp message received. messageId={}, from={}",
                    inbound.getMessageId(), phoneNumberMasker.mask(inbound.getFromPhone()));
            ChatMessage request = ChatMessage.builder()
                    .phoneNumber(inbound.getFromPhone())
                    .message(inbound.getTextBody())
                    .channel(CHANNEL_WHATSAPP)
                    .externalMessageId(inbound.getMessageId())
                    .build();

            conversationProcessingLockService.executeLocked(inbound.getFromPhone(), () -> {
                try {
                    ChatResult response = chatService.handleMessage(request);
                    metrics.inboundMessage(CHANNEL_WHATSAPP, response != null && response.getReply() == null ? "processed_no_reply" : "processed");
                    if (response != null && response.getSession() != null && !isBlank(response.getReply())) {
                        log.info("Dispatching WhatsApp reply. inboundMessageId={}, sessionId={}, to={}",
                                inbound.getMessageId(), response.getSession().getId(),
                                phoneNumberMasker.mask(response.getSession().getPhoneNumber()));
                        var outbound = outboundMessageService.sendBotReply(response.getSession(), response.getReply());
                        if (outbound.getStatus() == OutboundMessageStatus.FAILED) {
                            log.warn("Outbound WhatsApp reply persisted as FAILED. outboundId={}, sessionId={}, from={}, reason={}",
                                    outbound.getId(),
                                    response.getSession().getId(),
                                    phoneNumberMasker.mask(inbound.getFromPhone()),
                                    outbound.getLastError());
                        }
                    }
                } catch (InvalidChatMessageException ex) {
                    metrics.inboundMessage(CHANNEL_WHATSAPP, "invalid");
                    log.warn("Ignored inbound WhatsApp message with invalid payload. from={}, messageId={}",
                            phoneNumberMasker.mask(inbound.getFromPhone()), inbound.getMessageId());
                } catch (Exception ex) {
                    metrics.inboundMessage(CHANNEL_WHATSAPP, "failed");
                    log.error("Failed processing inbound WhatsApp message. from={}, messageId={}, cause={}",
                            phoneNumberMasker.mask(inbound.getFromPhone()), inbound.getMessageId(), ex.getMessage(), ex);
                }
            });
        }

        if (!inboundMessages.isEmpty()) {
            log.info("Processed {} inbound WhatsApp messages.", inboundMessages.size());
        }
        return inboundMessages.size();
    }

    private JsonNode parsePayload(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            log.warn("Invalid WhatsApp webhook payload. Cause: {}", e.getMessage());
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
