package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class InformationalReplyFactory {

    private final BeautyBotProperties properties;
    private final AvailabilityReplyFactory availabilityReplyFactory;

    public String build(MessageAnalysis analysis) {
        List<String> parts = new ArrayList<>();
        if (analysis.hasIntent(Intent.GREETING)) {
            parts.add(random(List.of("Hola, como andas?", "Buen dia.", "Hola, gracias por escribir.")));
        }
        if (analysis.hasIntent(Intent.LOCATION_QUESTION)) {
            parts.add(random(List.of(
                    "Estamos en " + properties.getLocation() + ".",
                    "La sede configurada es " + properties.getLocation() + ".",
                    "Nos encontramos en " + properties.getLocation() + "."
            )));
        }
        if (analysis.hasIntent(Intent.OPENING_HOURS_QUESTION)) {
            parts.add(random(List.of(
                    "Atendemos " + properties.getOpeningHours() + " con " + properties.getAttendingDoctor() + ".",
                    "El horario de atencion es " + properties.getOpeningHours() + " y atiende " + properties.getAttendingDoctor() + ".",
                    "Nuestros horarios de atencion son " + properties.getOpeningHours() + ". Atiende " + properties.getAttendingDoctor() + "."
            )));
        }
        if (analysis.hasIntent(Intent.TREATMENT_INFO)) {
            parts.add(treatmentInfoReply(analysis));
        }
        if (analysis.hasIntent(Intent.PRICE_QUESTION)) {
            parts.add(random(List.of(
                    "Los valores y promos los valida una asesora segun el tratamiento y la forma de pago.",
                    "Para pasarte valores exactos, una asesora revisa la promo vigente y la forma de pago.",
                    "Los precios los confirma una asesora con la promo vigente y el tratamiento que quieras realizar."
            )));
        }
        if (analysis.hasIntent(Intent.AVAILABILITY_QUESTION)) {
            parts.add(availabilityReplyFactory.build(analysis.getRawMessage()));
        }

        if (parts.isEmpty()) {
            parts.add(random(List.of(
                    "Contame que necesitas consultar y te ayudo.",
                    "Decime que informacion buscas y lo revisamos.",
                    "Estoy para ayudarte con la consulta."
            )));
        }
        return String.join(" ", parts);
    }

    private String treatmentInfoReply(MessageAnalysis analysis) {
        if (analysis.getTreatment() != null && !analysis.getTreatment().isBlank()) {
            return "Sobre " + analysis.getTreatment() + ", el detalle exacto lo valida una asesora segun tu caso.";
        }
        return random(List.of(
                "Ese detalle lo valida una asesora segun el tratamiento.",
                "Para ese punto, una asesora te confirma la informacion exacta.",
                "Ese dato lo revisa una asesora para responderte con precision."
        ));
    }

    private String random(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
