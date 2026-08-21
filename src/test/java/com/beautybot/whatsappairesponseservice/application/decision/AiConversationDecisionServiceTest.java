package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.assertj.core.api.Assertions.assertThat;

class AiConversationDecisionServiceTest {

    @Test
    void sendsLocalRulesAsInstructionsAndContextAsInput() throws Exception {
        BeautyBotProperties properties = new BeautyBotProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AiConversationDecisionService service = new AiConversationDecisionService(
                properties, promptProvider("fixed decision rules"), objectMapper, null, null, null, null);

        Map<String, Object> request = service.buildRequest(ConversationContext.builder().build(), "gpt-test");

        assertThat(request)
                .containsEntry("model", "gpt-test")
                .containsEntry("instructions", "fixed decision rules")
                .doesNotContainKey("prompt");
        assertThat(request.get("input").toString()).contains("\"currentMessage\" : null");
    }

    @Test
    void usesLocalPromptFingerprintAsSharedCacheKey() {
        BeautyBotProperties properties = new BeautyBotProperties();
        properties.getAi().setPromptCacheRetention("24h");
        AiDecisionPromptProvider promptProvider = promptProvider("fixed decision rules");
        AiConversationDecisionService service = new AiConversationDecisionService(
                properties, promptProvider, null, null, null, null, null);
        Map<String, Object> request = new LinkedHashMap<>();

        service.applyCacheSettings(request, "decision");

        assertThat(request.get("prompt_cache_key").toString())
                .startsWith("decision:")
                .endsWith(promptProvider.fingerprint());
        assertThat(request).containsEntry("prompt_cache_retention", "24h");
    }

    @Test
    void changesCacheKeyWhenLocalPromptChanges() {
        BeautyBotProperties properties = new BeautyBotProperties();
        AiConversationDecisionService firstService = new AiConversationDecisionService(
                properties, promptProvider("first rules"), null, null, null, null, null);
        AiConversationDecisionService secondService = new AiConversationDecisionService(
                properties, promptProvider("updated rules"), null, null, null, null, null);
        Map<String, Object> firstRequest = new LinkedHashMap<>();
        Map<String, Object> secondRequest = new LinkedHashMap<>();

        firstService.applyCacheSettings(firstRequest, "decision");
        secondService.applyCacheSettings(secondRequest, "decision");

        assertThat(firstRequest.get("prompt_cache_key").toString()).startsWith("decision:");
        assertThat(firstRequest.get("prompt_cache_key"))
                .isNotEqualTo(secondRequest.get("prompt_cache_key"));
    }

    @Test
    void includesAvailabilityResultInDynamicContext() throws Exception {
        BeautyBotProperties properties = new BeautyBotProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        AiConversationDecisionService service = new AiConversationDecisionService(
                properties, promptProvider("fixed decision rules"), objectMapper, null, null, null, null);
        ConversationContext context = ConversationContext.builder()
                .availabilitySuggestions(List.of("Viernes 18:00"))
                .availabilityLookupFailed(true)
                .build();

        JsonNode payload = objectMapper.readTree(service.buildUserInput(context));

        assertThat(payload.path("availabilitySuggestions").get(0).asText()).isEqualTo("Viernes 18:00");
        assertThat(payload.path("availabilityLookupFailed").asBoolean()).isTrue();
    }

    private AiDecisionPromptProvider promptProvider(String instructions) {
        return new AiDecisionPromptProvider(new ByteArrayResource(instructions.getBytes(UTF_8)));
    }
}
