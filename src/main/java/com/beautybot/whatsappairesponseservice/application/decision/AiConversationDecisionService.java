package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.ai.openai.OpenAiResponseParser;
import com.beautybot.whatsappairesponseservice.application.exception.AppException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.config.RestClientFactory;
import com.beautybot.whatsappairesponseservice.external.ExternalCallResultClassifier;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.DecisionSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationDecisionService implements ConversationDecisionService {

    private final BeautyBotProperties properties;
    private final AiDecisionPromptProvider promptProvider;
    private final ObjectMapper objectMapper;
    private final OpenAiResponseParser responseParser;
    private final RestClientFactory restClientFactory;
    private final ExternalCallResultClassifier externalCallResultClassifier;
    private final BeautyBotMetrics metrics;

    @Override
    public ConversationDecision decide(ConversationContext context) {
        BeautyBotProperties.Ai ai = properties.getAi();
        if (!ai.isEnabled() || ai.getApiKey() == null || ai.getApiKey().isBlank()) {
            throw new AppException(ResponseCode.AI_DECISION_CONFIGURATION_ERROR);
        }
        try {
            JsonNode response = restClientFactory.openAiClient(ai).post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(buildRequest(context, ai.getModel()))
                    .retrieve()
                    .body(JsonNode.class);

            String text = responseParser.extractText(response)
                    .map(this::stripJsonFences)
                    .orElseThrow(() -> new IllegalStateException("AI response did not contain text"));

            ConversationDecision decision = objectMapper.readValue(text, ConversationDecision.class);
            decision.setSource(DecisionSource.AI);
            metrics.aiDecision("success");
            metrics.externalCall("openai", "success");
            return decision;
        } catch (Exception e) {
            String result = externalCallResultClassifier.classify(e);
            metrics.aiDecision(result);
            metrics.externalCall("openai", result);
            log.warn("AI conversation decision call failed. result={}, cause={}", result, e.getMessage());
            throw new AppException(ResponseCode.AI_DECISION_REQUEST_FAILED, e, result);
        }
    }

    Map<String, Object> buildRequest(ConversationContext context, String model) throws JsonProcessingException {
        String contextualPayload = buildUserInput(context);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("store", false);
        applyCacheSettings(request, "decision");
        request.put("instructions", promptProvider.instructions());
        request.put("input", contextualPayload);
        return request;
    }

    void applyCacheSettings(Map<String, Object> request, String flow) {
        request.put("prompt_cache_key", flow + ":" + promptProvider.fingerprint());

        String retention = properties.getAi().getPromptCacheRetention();
        if (hasText(retention)) {
            request.put("prompt_cache_retention", retention.trim());
        }
    }

    String buildUserInput(ConversationContext context) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clinic", context.getClinic());
        payload.put("botCapabilities", context.getBotCapabilities());
        payload.put("currentSession", context.getCurrentSession());
        payload.put("currentMessage", context.getCurrentMessage());
        payload.put("lastUserMessage", context.getCurrentMessage() == null ? null : context.getCurrentMessage().getMessage());
        payload.put("lastBotMessage", context.getLastBotMessage());
        payload.put("recentMessages", context.getRecentMessages());
        payload.put("availabilitySuggestions", context.getAvailabilitySuggestions());
        payload.put("availabilityLookupFailed", context.isAvailabilityLookupFailed());
        payload.put("activePromotions", context.getActivePromotions());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    private String stripJsonFences(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?", "").trim();
            normalized = normalized.replaceFirst("```$", "").trim();
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
