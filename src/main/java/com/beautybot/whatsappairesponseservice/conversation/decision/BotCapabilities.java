package com.beautybot.whatsappairesponseservice.conversation.decision;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BotCapabilities {
    private boolean canCollectLeadData;
    private boolean canNotifyHuman;
    private boolean canConfirmAppointment;
    private boolean canCheckAvailability;
    private boolean canGiveExactPrices;
    private boolean canProvideMedicalAdvice;
    private boolean canCancelAppointments;
    private boolean canRescheduleAppointments;
}
