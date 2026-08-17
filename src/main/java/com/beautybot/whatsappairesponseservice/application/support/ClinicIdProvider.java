package com.beautybot.whatsappairesponseservice.application.support;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClinicIdProvider {

    private final BeautyBotProperties properties;

    public Long currentClinicId() {
        Long clinicId = properties.getClinicId();
        if (clinicId == null || clinicId <= 0) {
            throw new IllegalStateException("beauty-bot.clinic-id must be greater than zero.");
        }
        return clinicId;
    }
}
