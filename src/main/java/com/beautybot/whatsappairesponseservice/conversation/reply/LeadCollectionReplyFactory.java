package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.calendar.AvailabilitySlot;
import com.beautybot.whatsappairesponseservice.calendar.CalendarAvailabilityService;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class LeadCollectionReplyFactory {

    private final CalendarAvailabilityService calendarAvailabilityService;

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
        List<AvailabilitySlot> slots = calendarAvailabilityService.findNextAvailableSlots(3);
        if (!slots.isEmpty()) {
            return firstTimePrefix
                    + "Te puedo ofrecer "
                    + formatSlots(slots)
                    + ". Te queda comodo alguno de esos horarios?";
        }
        return random(List.of(
                firstTimePrefix + "Gracias" + nameSuffix(session) + ". Que dia u horario te queda comodo?",
                firstTimePrefix + "Dale" + nameSuffix(session) + ". Contame que disponibilidad horaria tenes para la consulta.",
                firstTimePrefix + "Perfecto" + nameSuffix(session) + ". Que momento te resulta mas conveniente?"
        ));
    }

    private String formatSlots(List<AvailabilitySlot> slots) {
        Locale locale = Locale.forLanguageTag("es-AR");
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE dd/MM", locale);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale);
        DateTimeFormatter fallbackFormatter = DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", locale);
        LocalDate firstDate = slots.getFirst().start().toLocalDate();
        boolean sameDay = slots.stream().allMatch(slot -> slot.start().toLocalDate().equals(firstDate));
        if (sameDay) {
            String day = slots.getFirst().start().format(dayFormatter).toLowerCase(Locale.ROOT);
            List<String> times = slots.stream().map(slot -> slot.start().format(timeFormatter)).toList();
            return day + " a las " + joinReadable(times);
        }
        return joinReadable(slots.stream()
                .map(slot -> slot.start().format(fallbackFormatter).toLowerCase(Locale.ROOT))
                .toList());
    }

    private String joinReadable(List<String> items) {
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.getFirst();
        if (items.size() == 2) return items.get(0) + " y " + items.get(1);
        return String.join(", ", items.subList(0, items.size() - 1)) + " y " + items.getLast();
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
