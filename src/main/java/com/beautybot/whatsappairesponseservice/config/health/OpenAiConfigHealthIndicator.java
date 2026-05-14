package com.beautybot.whatsappairesponseservice.config.health;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("openAiConfig")
@RequiredArgsConstructor
public class OpenAiConfigHealthIndicator implements HealthIndicator {

    private final BeautyBotProperties properties;

    @Override
    public Health health() {
        BeautyBotProperties.Ai ai = properties.getAi();
        if (ai == null || !ai.isEnabled()) {
            return Health.up().withDetail("enabled", false).build();
        }
        if (ai.getApiKey() == null || ai.getApiKey().isBlank()) {
            return Health.down().withDetail("enabled", true).withDetail("reason", "OPENAI_API_KEY missing").build();
        }
        return Health.up().withDetail("enabled", true).withDetail("model", ai.getModel()).build();
    }
}
