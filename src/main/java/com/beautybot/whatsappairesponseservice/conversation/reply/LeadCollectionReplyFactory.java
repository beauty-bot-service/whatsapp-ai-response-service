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
                    "Perfecto. Indica tu nombre para registrar la consulta.",
                    "Por favor, indica tu nombre para dejar el registro.",
                    "Necesito tu nombre para continuar con la gestion de la consulta."
            ));
        }
        if (field == RequiredField.FIRST_TIME) {
            return random(List.of(
                    "Perfecto. Es tu primera vez en la clinica?",
                    "Para registrarlo correctamente, confirma si es tu primera vez en la clinica?",
                    "Confirma por favor si ya eres paciente de la clinica o si es tu primera consulta?"
            ));
        }
        if (field == RequiredField.PREFERRED_TIME) {
            return askPreferredTime(session);
        }
        return random(List.of(
                "Hola. Te ayudo con la gestion de tu consulta. Que tratamiento te interesa?",
                "Hola. Para continuar, indica que tratamiento te interesa consultar.",
                "Hola. Indica que tratamiento deseas consultar y te acompano con la gestion."
        ));
    }

    public String readyForHuman(ConversationSession session) {
        String firstTimeText = Boolean.TRUE.equals(session.getFirstTime())
                ? "que es tu primera consulta en la clinica"
                : "que ya eres paciente de la clinica";
        return random(List.of(
                "Perfecto" + nameSuffix(session) + ". Registro que deseas consultar por "
                        + session.getTreatmentInterest() + ", " + firstTimeText
                        + " y que prefieres " + session.getPreferredTime() + ". Una asesora te contactara para continuar.",
                "Listo" + nameSuffix(session) + ". Queda registrado que te interesa "
                        + session.getTreatmentInterest() + ", " + firstTimeText
                        + " y que te resulta conveniente " + session.getPreferredTime() + ". Una asesora te contactara para coordinar.",
                "Registro completo" + nameSuffix(session) + ". Queda cargado que consultas por "
                        + session.getTreatmentInterest() + ", " + firstTimeText
                        + " y con preferencia " + session.getPreferredTime() + ". Una asesora te escribira para avanzar."
        ));
    }

    private String askPreferredTime(ConversationSession session) {
        List<AvailabilitySlot> slots = calendarAvailabilityService.findNextAvailableSlots(3);
        if (!slots.isEmpty()) {
            return "Gracias" + nameSuffix(session)
                    + ". Tengo disponibilidad en "
                    + formatSlots(slots)
                    + ". Indica si alguno de esos horarios te sirve o comparti otra preferencia.";
        }
        return random(List.of(
                "Gracias" + nameSuffix(session) + ". Indica un dia u horario de preferencia. Si lo deseas, una asesora puede contactarte para coordinar.",
                "Gracias" + nameSuffix(session) + ". Cual es tu disponibilidad horaria para la consulta?",
                "Para completar el registro, indicanos que dia u horario te resulta mas conveniente."
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
