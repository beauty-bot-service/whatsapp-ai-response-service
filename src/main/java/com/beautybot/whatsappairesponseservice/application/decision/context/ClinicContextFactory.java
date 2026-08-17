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
                .attendingDoctor(properties.getAttendingDoctor())
                .pricePolicy("Los precios pueden depender de la evaluacion profesional, tratamiento, zona y cantidad de sesiones.")
                .treatmentPolicy("El bot puede explicar tratamientos de forma general y breve, pero no debe diagnosticar, evaluar riesgos ni recomendar tratamientos personalizados. Las consultas clinicas, profundas o serias se derivan a una persona.")
                .schedulingPolicy("Informar dias, horarios y profesional de atencion. Pedir una fecha de preferencia para registrar el lead, sin consultar, ofrecer ni confirmar disponibilidad de calendario.")
                .build();
    }
}
