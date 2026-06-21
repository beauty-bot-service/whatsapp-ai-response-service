package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class HandoffReplyFactory {

    public String build(MessageAnalysis analysis) {
        Intent intent = analysis == null ? Intent.UNKNOWN : analysis.getIntent();
        String normalized = normalize(analysis == null ? null : analysis.getRawMessage());
        if (isMediaRequest(normalized)) {
            return random(List.of(
                    "Ahora no puedo enviar fotos desde el bot. Derivo la conversacion para que una asesora te comparta material y siga con la consulta.",
                    "Te derivo con una asesora para que pueda pasarte fotos del tratamiento y responderte bien.",
                    "Para enviarte imagenes y ejemplos, te comunico con una asesora."
            ));
        }
        if (isPaymentOrConfirmation(normalized)) {
            return random(List.of(
                    "Perfecto. Derivo con una asesora para validar la informacion y confirmar el turno.",
                    "Dale. Te comunico con una asesora para revisar la confirmacion y seguir con la agenda.",
                    "Recibido. Dejo el caso con una asesora para validar y confirmar la coordinacion."
            ));
        }
        if (intent == Intent.COMPLAINT) {
            return random(List.of(
                    "Lamento la demora. Derivo la conversacion con una asesora para que te den seguimiento.",
                    "Entiendo. Te comunico con una asesora para revisar el caso y continuar.",
                    "Gracias por avisar. Dejo el caso derivado para que una asesora lo revise."
            ));
        }
        if (intent == Intent.MEDICAL_QUESTION) {
            return random(List.of(
                    "Esta consulta necesita revision profesional. Te derivo con una asesora para seguirla correctamente.",
                    "Para responderte bien en este punto, te comunico con una asesora.",
                    "Ese tema conviene revisarlo con el equipo. Derivo la conversacion para que te den seguimiento."
            ));
        }
        if (intent == Intent.RESCHEDULE) {
            return random(List.of(
                    "Para cambiar el turno, te derivo con una asesora para revisar la agenda.",
                    "Te comunico con una asesora para ver opciones de reprogramacion.",
                    "Dejo tu solicitud con una asesora para mover el turno segun disponibilidad."
            ));
        }
        if (intent == Intent.CANCEL) {
            return random(List.of(
                    "Te derivo con una asesora para gestionar la cancelacion.",
                    "Te comunico con una asesora para avanzar con la cancelacion del turno.",
                    "Dejo el caso con una asesora para revisar la cancelacion."
            ));
        }
        if (intent == Intent.HUMAN_REQUEST) {
            return random(List.of(
                    "Dale. Te comunico con una asesora para continuar.",
                    "Perfecto. Derivo la conversacion con una asesora.",
                    "Te paso con una asesora para seguir con la gestion."
            ));
        }
        return random(List.of(
                "Te derivo con una asesora para continuar.",
                "Dejo el caso con una asesora para darte seguimiento.",
                "Te comunico con una asesora para revisar la consulta."
        ));
    }

    private boolean isMediaRequest(String normalized) {
        return containsAny(normalized, List.of(
                "foto", "fotos", "imagen", "imagenes", "video", "videos",
                "trabajos", "resultados", "antes y despues", "ejemplos"
        ));
    }

    private boolean isPaymentOrConfirmation(String normalized) {
        return containsAny(normalized, List.of(
                "sena", "senar", "comprobante", "transferencia", "pago", "pague",
                "abone", "confirmo", "confirmar turno", "confirmacion", "cbu", "alias"
        ));
    }

    private boolean containsAny(String text, List<String> terms) {
        return terms.stream().anyMatch(text::contains);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private String random(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
