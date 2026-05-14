package com.beautybot.whatsappairesponseservice.application.decision.context;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.decision.ClinicContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClinicContextFactory {

    private final BeautyBotProperties properties;

    public ClinicContext build() {
        return ClinicContext.builder()
                .name(properties.getClinicName())
                .location(properties.getLocation())
                .openingHours(properties.getOpeningHours())
                .pricePolicy("Los precios pueden depender de la evaluacion profesional, tratamiento, zona y cantidad de sesiones.")
                .treatmentPolicy("El bot puede dar informacion general, pero no debe diagnosticar ni recomendar tratamientos medicos personalizados.")
                .build();
    }
}
