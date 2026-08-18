package com.beautybot.whatsappairesponseservice.whatsapp;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.config.RestClientFactory;
import com.beautybot.whatsappairesponseservice.external.ExternalCallResultClassifier;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.beautybot.whatsappairesponseservice.observability.PhoneNumberMasker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

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
    private final PhoneNumberMasker phoneNumberMasker;
    private final ObjectMapper objectMapper;

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
            Map<String, Object> payload = buildTextMessagePayload(toPhoneNumber, body);

            log.info("Sending outbound WhatsApp message. to={}", phoneNumberMasker.mask(toPhoneNumber));
            if (whatsapp.isLogPayloads()) {
                log.info("Outbound WhatsApp API payload. payload={}", toJson(payload));
            }

            restClient.post()
                    .uri("/{phoneNumberId}/messages", whatsapp.getPhoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            metrics.externalCall("whatsapp", "success");
            log.info("Outbound WhatsApp API request accepted. to={}", phoneNumberMasker.mask(toPhoneNumber));
            return WhatsAppSendResult.SENT;
        } catch (Exception e) {
            String result = externalCallResultClassifier.classify(e);
            metrics.externalCall("whatsapp", result);
            logSendFailure(toPhoneNumber, result, e);
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

    private void logSendFailure(String toPhoneNumber, String result, Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            log.warn("Failed to send outbound WhatsApp message. to={}, result={}, httpStatus={}, metaResponse={}",
                    phoneNumberMasker.mask(toPhoneNumber), result, responseException.getStatusCode().value(),
                    responseException.getResponseBodyAsString());
            return;
        }
        log.warn("Failed to send outbound WhatsApp message. to={}, result={}, exceptionType={}, cause={}",
                phoneNumberMasker.mask(toPhoneNumber), result, exception.getClass().getSimpleName(),
                exception.getMessage(), exception);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"payload could not be serialized\"}";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

