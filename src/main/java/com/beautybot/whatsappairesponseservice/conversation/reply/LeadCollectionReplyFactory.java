package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class LeadCollectionReplyFactory {

    private final BeautyBotProperties properties;

    public String askFor(RequiredField field, ConversationSession session) {
        if (field == RequiredField.NAME) {
            return random(List.of(
                    "Perfecto. Me pasas tu nombre completo para dejarlo registrado?",
                    "Dale. A nombre de quien dejamos la consulta?",
                    "Bien. Decime tu nombre completo y lo dejo asentado para la asesora."
            ));
        }
        if (field == RequiredField.FIRST_TIME) {
            return random(List.of(
                    "Perfecto. Es tu primera vez en Dr.Beauty?",
                    "Bien. Ya te atendiste antes con nosotros o seria tu primera vez?",
                    "Dale. Te consulto, seria tu primera consulta en la clinica?"
            ));
        }
        if (field == RequiredField.PREFERRED_TIME) {
            return askPreferredTime(session);
        }
        return random(List.of(
                "Hola, como andas? Contame que tratamiento te interesa y te ayudo.",
                "Hola. Dale, te ayudo. Que tratamiento estas buscando?",
                "Hola, gracias por escribir. Contame que tratamiento te interesa."
        ));
    }

    public String readyForHuman(ConversationSession session) {
        return random(List.of(
                "Perfecto" + nameSuffix(session) + ". Dejo tu consulta registrada para que una asesora revise la agenda y te escriba para coordinar.",
                "Listo" + nameSuffix(session) + ". Queda cargada tu consulta y una asesora te va a contactar para avanzar.",
                "Perfecto" + nameSuffix(session) + ". Ya dejo los datos para que una asesora continue con la coordinacion."
        ));
    }

    private String askPreferredTime(ConversationSession session) {
        String firstTimePrefix = Boolean.TRUE.equals(session.getFirstTime())
                ? "Al ser tu primera vez, podemos contemplar una consulta previa para que te saques dudas. "
                : "";
        String attentionDetails = "Atendemos " + properties.getOpeningHours()
                + " y atiende " + properties.getAttendingDoctor() + ". ";
        return random(List.of(
                firstTimePrefix + attentionDetails + "Gracias" + nameSuffix(session) + ". Que fecha preferis para dejarla registrada?",
                firstTimePrefix + attentionDetails + "Dale" + nameSuffix(session) + ". Decime que fecha te queda mejor y una asesora la revisa.",
                firstTimePrefix + attentionDetails + "Perfecto" + nameSuffix(session) + ". Que fecha de preferencia queres que anotemos?"
        ));
    }

    private String nameSuffix(ConversationSession session) {
        return session.getCustomerName() == null || session.getCustomerName().isBlank()
                ? ""
                : ", " + session.getCustomerName();
    }

    private String random(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
