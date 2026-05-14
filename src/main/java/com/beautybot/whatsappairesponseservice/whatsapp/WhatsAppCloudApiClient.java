package com.beautybot.whatsappairesponseservice.whatsapp;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.config.RestClientFactory;
import com.beautybot.whatsappairesponseservice.external.ExternalCallResultClassifier;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppCloudApiClient {

    private final BeautyBotProperties properties;
    private final RestClientFactory restClientFactory;
    private final ExternalCallResultClassifier externalCallResultClassifier;
    private final BeautyBotMetrics metrics;

    public WhatsAppSendResult sendTextMessage(String toPhoneNumber, String body) {
        BeautyBotProperties.Whatsapp whatsapp = properties.getWhatsapp();
        if (!whatsapp.isEnabled()) {
            return WhatsAppSendResult.DISABLED;
        }

        if (isBlank(whatsapp.getAccessToken()) || isBlank(whatsapp.getPhoneNumberId())) {
            log.warn("WhatsApp enabled but access-token or phone-number-id is missing. Outbound message skipped.");
            return WhatsAppSendResult.NOT_CONFIGURED;
        }

        if (isBlank(toPhoneNumber) || isBlank(body)) {
            return WhatsAppSendResult.INVALID_INPUT;
        }

        try {
            RestClient restClient = restClientFactory.whatsappClient(whatsapp);

            restClient.post()
                    .uri("/{phoneNumberId}/messages", whatsapp.getPhoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildTextMessagePayload(toPhoneNumber, body))
                    .retrieve()
                    .toBodilessEntity();
            metrics.externalCall("whatsapp", "success");
            return WhatsAppSendResult.SENT;
        } catch (Exception e) {
            String result = externalCallResultClassifier.classify(e);
            metrics.externalCall("whatsapp", result);
            log.warn("Failed to send outbound WhatsApp message. result={}, cause={}", result, e.getMessage());
            return toWhatsAppSendResult(e, result);
        }
    }

    private WhatsAppSendResult toWhatsAppSendResult(Exception exception, String classifiedResult) {
        if ("rate_limited".equals(classifiedResult)) {
            return WhatsAppSendResult.RATE_LIMITED;
        }
        if ("unauthorized".equals(classifiedResult)) {
            return WhatsAppSendResult.UNAUTHORIZED;
        }
        if ("forbidden".equals(classifiedResult)) {
            return WhatsAppSendResult.FORBIDDEN;
        }
        if (exception instanceof ResourceAccessException) {
            return WhatsAppSendResult.TIMEOUT;
        }
        if (exception instanceof HttpClientErrorException.TooManyRequests) {
            return WhatsAppSendResult.RATE_LIMITED;
        }
        if (exception instanceof HttpClientErrorException.Unauthorized) {
            return WhatsAppSendResult.UNAUTHORIZED;
        }
        if (exception instanceof HttpClientErrorException.Forbidden) {
            return WhatsAppSendResult.FORBIDDEN;
        }
        if (exception instanceof HttpServerErrorException) {
            return WhatsAppSendResult.SERVER_ERROR;
        }
        return WhatsAppSendResult.FAILED;
    }

    public boolean isConfigured() {
        BeautyBotProperties.Whatsapp whatsapp = properties.getWhatsapp();
        return whatsapp.isEnabled()
                && !isBlank(whatsapp.getAccessToken())
                && !isBlank(whatsapp.getPhoneNumberId());
    }

    private Map<String, Object> buildTextMessagePayload(String toPhoneNumber, String body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", toPhoneNumber);
        payload.put("type", "text");
        payload.put("text", Map.of(
                "preview_url", false,
                "body", body
        ));
        return payload;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


