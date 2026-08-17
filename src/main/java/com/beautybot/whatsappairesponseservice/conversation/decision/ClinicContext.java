package com.beautybot.whatsappairesponseservice.conversation.decision;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClinicContext {
    private String name;
    private String location;
    private String openingHours;
    private String attendingDoctor;
    private String pricePolicy;
    private String treatmentPolicy;
    private String schedulingPolicy;
}
