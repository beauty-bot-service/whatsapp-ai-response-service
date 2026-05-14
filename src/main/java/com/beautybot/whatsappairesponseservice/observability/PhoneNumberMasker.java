package com.beautybot.whatsappairesponseservice.observability;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PhoneNumberMasker {

    private final BeautyBotProperties properties;

    public String mask(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }
        if (properties.getObservability() != null && !properties.getObservability().isMaskPhoneNumbers()) {
            return phoneNumber;
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "****" + digits.substring(digits.length() - 4);
    }
}
