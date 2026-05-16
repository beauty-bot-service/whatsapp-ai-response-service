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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationDecisionService implements ConversationDecisionService {

    private final BeautyBotProperties properties;
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

    private Map<String, Object> buildRequest(ConversationContext context, String model) throws JsonProcessingException {
        boolean promptTemplateMode = isPromptTemplateConfigured();
        String contextualPayload = buildUserInput(context);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("store", false);
        applyCacheSettings(request, context, "decision");
        if (promptTemplateMode) {
            request.put("prompt", buildPromptObject(contextualPayload));
        } else {
            request.put("instructions", buildInstructions());
            request.put("input", List.of(Map.of(
                    "role", "user",
                    "content", contextualPayload
            )));
        }
        return request;
    }

    private boolean isPromptTemplateConfigured() {
        BeautyBotProperties.Ai ai = properties.getAi();
        return ai != null && ai.getDecisionPrompt() != null && ai.getDecisionPrompt().isConfigured();
    }

    private Map<String, Object> buildPromptObject(String contextualPayload) {
        BeautyBotProperties.Ai.PromptTemplate promptTemplate = properties.getAi().getDecisionPrompt();
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("id", promptTemplate.getId().trim());
        if (promptTemplate.getVersion() != null && !promptTemplate.getVersion().isBlank()) {
            prompt.put("version", promptTemplate.getVersion().trim());
        }
        prompt.put("variables", Map.of("conversation_context", contextualPayload));
        return prompt;
    }

    private void applyCacheSettings(Map<String, Object> request, ConversationContext context, String flow) {
        String phoneNumber = context == null || context.getCurrentSession() == null
                ? null
                : context.getCurrentSession().getPhoneNumber();
        String normalizedPhone = normalizePhone(phoneNumber);
        if (hasText(normalizedPhone)) {
            request.put("prompt_cache_key", flow + ":" + normalizedPhone);
        }

        String retention = properties.getAi().getPromptCacheRetention();
        if (hasText(retention)) {
            request.put("prompt_cache_retention", retention.trim());
        }
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private String buildInstructions() { // TODO revisar
        return String.join("\n",
                "Sos el motor de decision conversacional de un chatbot de WhatsApp para una clinica estetica.",
                "Tu tarea es interpretar el mensaje actual usando el contexto y devolver SOLO JSON valido.",
                "No uses markdown. No expliques fuera del JSON. No inventes datos.",
                "Cuando shouldBotReply sea true, el texto de reply debe tener tono serio, profesional y humano, como personal administrativo.",
                "No uses emojis, no uses signos de exclamacion y no uses el signo de apertura de interrogacion.",
                "Interpreta fechas numericas como dd/MM (formato Argentina), salvo que el cliente indique el anio de forma explicita.",
                "Si el contexto incluye availabilitySuggestions, usalas para responder disponibilidad. No inventes horarios fuera de esa lista.",
                "Cuando availabilitySuggestions tenga rangos por dia, manten esa estructura en la respuesta y evita enumerar slots de 30 minutos uno por uno.",
                "Debes decidir el proximo estado conversacional, datos extraidos, acciones y la respuesta exacta a enviar si corresponde.",
                "El backend ejecutara y validara la decision; no incluyas texto fuera del objeto JSON."
        );
    }

    private String buildUserInput(ConversationContext context) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clinic", context.getClinic());
        payload.put("botCapabilities", context.getBotCapabilities());
        payload.put("currentSession", context.getCurrentSession());
        payload.put("currentMessage", context.getCurrentMessage());
        payload.put("lastUserMessage", context.getCurrentMessage() == null ? null : context.getCurrentMessage().getMessage());
        payload.put("lastBotMessage", context.getLastBotMessage());
        payload.put("recentMessages", context.getRecentMessages());
        payload.put("allowedStates", context.getAllowedStates());
        payload.put("allowedIntents", context.getAllowedIntents());
        payload.put("allowedWaitingFields", context.getAllowedWaitingFields());
        payload.put("requiredLeadFields", context.getRequiredLeadFields());
        payload.put("mandatoryRules", context.getMandatoryRules());
        payload.put("availabilityRequest", context.getAvailabilityRequest());
        payload.put("availabilitySuggestions", context.getAvailabilitySuggestions());
        payload.put("responseFormat", responseFormat());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    private Map<String, Object> responseFormat() {
        Map<String, Object> extractedData = new LinkedHashMap<>();
        extractedData.put("customerName", "string or null");
        extractedData.put("treatmentInterest", "string or null");
        extractedData.put("firstTime", "boolean or null");
        extractedData.put("preferredTime", "string or null");
        extractedData.put("contactPreference", "HUMAN_CONTACT | SPECIFIC_TIME | null");

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("intents", "array with one or more values from allowedIntents");
        format.put("nextState", "COLLECTING_DATA | READY_FOR_HUMAN | HUMAN_HANDOFF | CLOSED");
        format.put("nextWaitingForField", "TREATMENT | NAME | FIRST_TIME | PREFERRED_TIME | null");
        format.put("extractedData", extractedData);
        format.put("missingFields", "array of required fields still missing");
        format.put("requiresHuman", "boolean");
        format.put("shouldCreateLead", "boolean");
        format.put("shouldNotifyHuman", "boolean");
        format.put("shouldBotReply", "boolean");
        format.put("reply", "string or null. Exact text to send to the customer when shouldBotReply is true");
        format.put("summaryForHuman", "string or null. Short summary for advisor");
        format.put("decisionReason", "short explanation for logs");
        return format;
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
