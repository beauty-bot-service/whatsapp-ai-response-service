package com.beautybot.whatsappairesponseservice.application.decision.context;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.decision.BotCapabilities;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotCapabilitiesFactory {

    private final BeautyBotProperties properties;

    public BotCapabilities build() {
        BeautyBotProperties.BotCapabilitiesConfig capabilities = properties.getBotCapabilities() == null
                ? new BeautyBotProperties.BotCapabilitiesConfig()
                : properties.getBotCapabilities();
        return BotCapabilities.builder()
                .canCollectLeadData(capabilities.isCanCollectLeadData())
                .canNotifyHuman(capabilities.isCanNotifyHuman())
                .canConfirmAppointment(capabilities.isCanConfirmAppointment())
                .canCheckAvailability(capabilities.isCanCheckAvailability())
                .canGiveExactPrices(capabilities.isCanGiveExactPrices())
                .canProvideMedicalAdvice(capabilities.isCanProvideMedicalAdvice())
                .canCancelAppointments(capabilities.isCanCancelAppointments())
                .canRescheduleAppointments(capabilities.isCanRescheduleAppointments())
                .build();
    }
}
