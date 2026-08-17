package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityReplyFactory {

    private final BeautyBotProperties properties;

    public String build(String rawMessage) {
        return "Atendemos " + properties.getOpeningHours()
                + ". Atiende " + properties.getAttendingDoctor()
                + ". La fecha se coordina luego con una asesora, sin confirmarla automaticamente.";
    }
}
