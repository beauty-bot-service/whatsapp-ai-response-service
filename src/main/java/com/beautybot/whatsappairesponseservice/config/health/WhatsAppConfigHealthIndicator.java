package com.beautybot.whatsappairesponseservice.config.health;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("whatsAppConfig")
@RequiredArgsConstructor
public class WhatsAppConfigHealthIndicator implements HealthIndicator {

    private final BeautyBotProperties properties;

    @Override
    public Health health() {
        BeautyBotProperties.Whatsapp whatsapp = properties.getWhatsapp();
        if (whatsapp == null || !whatsapp.isEnabled()) {
            return Health.up().withDetail("enabled", false).build();
        }
        if (isBlank(whatsapp.getAccessToken()) || isBlank(whatsapp.getPhoneNumberId()) || isBlank(whatsapp.getVerifyToken())) {
            return Health.down().withDetail("enabled", true).withDetail("reason", "WhatsApp config incomplete").build();
        }
        return Health.up().withDetail("enabled", true).build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
