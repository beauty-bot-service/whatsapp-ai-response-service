package com.beautybot.whatsappairesponseservice.application.decision;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class AiDecisionPromptProvider {

    private final String instructions;
    private final String fingerprint;

    public AiDecisionPromptProvider(
            @Value("${beauty-bot.ai.decision-prompt-resource:classpath:prompts/ai-decision-prompt.txt}")
            Resource promptResource) {
        try {
            this.instructions = promptResource.getContentAsString(UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load AI decision prompt", e);
        }
        if (instructions.isBlank()) {
            throw new IllegalStateException("AI decision prompt must not be empty");
        }
        this.fingerprint = UUID.nameUUIDFromBytes(instructions.getBytes(UTF_8)).toString();
    }

    public String instructions() {
        return instructions;
    }

    public String fingerprint() {
        return fingerprint;
    }
}
