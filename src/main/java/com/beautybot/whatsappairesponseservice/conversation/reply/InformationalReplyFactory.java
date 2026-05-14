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
            parts.add(random(List.of("Hola.", "Buen dia.", "Hola, gracias por escribir.")));
        }
        if (analysis.hasIntent(Intent.LOCATION_QUESTION)) {
            parts.add(random(List.of(
                    "Estamos en " + properties.getLocation() + ".",
                    "La sede se encuentra en " + properties.getLocation() + ".",
                    "La clinica esta en " + properties.getLocation() + "."
            )));
        }
        if (analysis.hasIntent(Intent.OPENING_HOURS_QUESTION)) {
            parts.add(random(List.of(
                    "Atendemos " + properties.getOpeningHours() + ".",
                    "El horario de atencion es " + properties.getOpeningHours() + ".",
                    "Nuestros horarios de atencion son " + properties.getOpeningHours() + "."
            )));
        }
        if (analysis.hasIntent(Intent.PRICE_QUESTION)) {
            parts.add(random(List.of(
                    "Los valores pueden variar segun el tratamiento y la evaluacion.",
                    "El precio depende del tratamiento y de la evaluacion profesional.",
                    "Los costos se confirman en funcion del tratamiento y de la evaluacion."
            )));
        }
        if (analysis.hasIntent(Intent.AVAILABILITY_QUESTION)) {
            parts.add(availabilityReplyFactory.build(analysis.getRawMessage()));
        }

        if (parts.isEmpty()) {
            parts.add(random(List.of(
                    "Indicanos que informacion necesitas y te ayudo.",
                    "Indica que necesitas consultar y te respondo.",
                    "Estoy para ayudarte con la informacion que necesites."
            )));
        }
        parts.add(resolveClosing(analysis));
        return String.join(" ", parts);
    }

    private String resolveClosing(MessageAnalysis analysis) {
        if (analysis.hasIntent(Intent.AVAILABILITY_QUESTION)) {
            return "Si alguno de esos horarios te sirve, indicalo y avanzo con el registro.";
        }
        if (analysis.hasIntent(Intent.PRICE_QUESTION)) {
            return random(List.of(
                    "Si lo deseas, puedo registrar tus datos y una asesora te envia el detalle.",
                    "Si estas de acuerdo, dejo tu consulta registrada para que una asesora te contacte.",
                    "Tambien podemos registrar tus datos para que una asesora te comparta la informacion exacta."
            ));
        }
        return random(List.of(
                "Si lo deseas, tambien puedo registrar una consulta para que te contacten.",
                "Si te parece, continuamos y dejo la consulta registrada.",
                "Tambien puedo tomar tus datos y dejar la solicitud en gestion."
        ));
    }

    private String random(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
