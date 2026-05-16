package com.beautybot.whatsappairesponseservice.outbound.service.impl;

import com.beautybot.whatsappairesponseservice.application.exception.AppException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.beautybot.whatsappairesponseservice.observability.PhoneNumberMasker;
import com.beautybot.whatsappairesponseservice.outbound.model.OutboundMessage;
import com.beautybot.whatsappairesponseservice.outbound.service.OutboundMessageService;
import com.beautybot.whatsappairesponseservice.outbound.state.OutboundMessageStatus;
import com.beautybot.whatsappairesponseservice.persistence.repository.OutboundMessageModelRepository;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppCloudApiClient;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundMessageServiceImpl implements OutboundMessageService {

    private static final String CHANNEL_WHATSAPP = "WHATSAPP";
    private static final int LAST_ERROR_MAX_LENGTH = 1000;

    private final OutboundMessageModelRepository repository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final BeautyBotMetrics metrics;
    private final PhoneNumberMasker phoneNumberMasker;

    @Override
    public OutboundMessage sendBotReply(ConversationSession session, String content) {
        validateSession(session);
        String normalizedPhoneNumber = requireText(session.getPhoneNumber(), ResponseCode.OUTBOUND_SESSION_PHONE_REQUIRED);
        String normalizedContent = requireText(content, ResponseCode.OUTBOUND_CONTENT_REQUIRED);

        LocalDateTime now = LocalDateTime.now();
        OutboundMessage outbound = repository.save(OutboundMessage.builder()
                .sessionId(session.getId())
                .phoneNumber(normalizedPhoneNumber)
                .channel(CHANNEL_WHATSAPP)
                .content(normalizedContent)
                .status(OutboundMessageStatus.FAILED)
                .attemptCount(0)
                .lastError("dispatch_not_attempted")
                .createdAt(now)
                .updatedAt(now)
                .build());
        metrics.outbox("queued");

        WhatsAppSendResult result;
        String deliveryError = null;
        try {
            result = whatsAppCloudApiClient.sendTextMessage(normalizedPhoneNumber, normalizedContent);
        } catch (Exception ex) {
            result = WhatsAppSendResult.FAILED;
            deliveryError = ex.getMessage();
        }

        LocalDateTime dispatchedAt = LocalDateTime.now();
        outbound.setAttemptCount(1);
        outbound.setUpdatedAt(dispatchedAt);

        if (result == WhatsAppSendResult.SENT) {
            outbound.setStatus(OutboundMessageStatus.SENT);
            outbound.setSentAt(dispatchedAt);
            outbound.setLastError(null);
            OutboundMessage saved = repository.save(outbound);
            metrics.outboundMessage(saved.getChannel(), "sent");
            metrics.outbox("sent");
            log.info("Outbound WhatsApp message sent. id={}, phone={}, attempts={}",
                    saved.getId(), phoneNumberMasker.mask(saved.getPhoneNumber()), saved.getAttemptCount());
            return saved;
        }

        outbound.setStatus(OutboundMessageStatus.FAILED);
        outbound.setSentAt(null);
        outbound.setLastError(truncate(deliveryError == null ? result.name() : deliveryError, LAST_ERROR_MAX_LENGTH));
        OutboundMessage saved = repository.save(outbound);
        metrics.outboundMessage(saved.getChannel(), "failed");
        metrics.outbox("failed");
        log.warn("Outbound WhatsApp message failed. id={}, phone={}, attempts={}, result={}",
                saved.getId(), phoneNumberMasker.mask(saved.getPhoneNumber()), saved.getAttemptCount(), result);
        return saved;
    }

    private void validateSession(ConversationSession session) {
        if (session == null || session.getId() == null) {
            throw new AppException(ResponseCode.OUTBOUND_SESSION_ID_REQUIRED);
        }
    }

    private String requireText(String value, ResponseCode responseCode) {
        if (value == null || value.isBlank()) {
            throw new AppException(responseCode);
        }
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
