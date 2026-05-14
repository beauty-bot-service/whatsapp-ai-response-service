package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class HandoffReplyFactory {

    public String build(MessageAnalysis analysis) {
        Intent intent = analysis == null ? Intent.UNKNOWN : analysis.getIntent();
        if (intent == Intent.MEDICAL_QUESTION) {
            return random(List.of(
                    "Esta consulta requiere evaluacion profesional. Derivo tu mensaje para seguimiento por una asesora.",
                    "Para darte una respuesta precisa en este punto, derivo la conversacion con una asesora.",
                    "Para esta consulta te va a asistir mejor una asesora o un profesional. Derivo el caso para continuidad."
            ));
        }
        if (intent == Intent.RESCHEDULE) {
            return random(List.of(
                    "Para reprogramar tu turno, derivo el caso con una asesora para revisar disponibilidad.",
                    "Te comunico con una asesora para gestionar opciones de reprogramacion.",
                    "Dejo tu solicitud derivada para cambiar el turno segun agenda."
            ));
        }
        if (intent == Intent.CANCEL) {
            return random(List.of(
                    "Derivo tu solicitud con una asesora para gestionar la cancelacion.",
                    "Te comunico con una asesora para avanzar con la cancelacion.",
                    "Dejo el caso derivado para cancelar el turno."
            ));
        }
        if (intent == Intent.HUMAN_REQUEST) {
            return random(List.of(
                    "De acuerdo. Derivo la conversacion con una asesora para continuar.",
                    "Te comunico con una asesora para seguir con la gestion.",
                    "Entendido. Dejo el caso derivado para atencion de una asesora."
            ));
        }
        return random(List.of(
                "Derivo la consulta con una asesora para una atencion mas precisa.",
                "Dejo el caso en gestion con una asesora para continuar.",
                "Te comunico con una asesora para darte seguimiento."
        ));
    }

    private String random(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
