package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.calendar.AvailabilityRequest;
import com.beautybot.whatsappairesponseservice.calendar.AvailabilityRequestParser;
import com.beautybot.whatsappairesponseservice.calendar.AvailabilitySlot;
import com.beautybot.whatsappairesponseservice.calendar.CalendarAvailabilityService;
import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AvailabilityReplyFactory {

    private static final int FETCH_LIMIT = 240;
    private static final String DEFAULT_ZONE = "America/Argentina/Buenos_Aires";

    private final BeautyBotProperties properties;
    private final CalendarAvailabilityService calendarAvailabilityService;
    private final AvailabilityRequestParser availabilityRequestParser;

    public String build(String rawMessage) {
        if (!calendarAvailabilityService.isConfigured()) {
            return "La disponibilidad la valida una asesora segun agenda.";
        }

        AvailabilityRequest request = availabilityRequestParser.parse(rawMessage, resolveCalendarZoneId());
        if (request.hasDate()) {
            return buildDateSpecificReply(request);
        }

        List<AvailabilitySlot> slots = calendarAvailabilityService.findNextAvailableSlots(FETCH_LIMIT);
        if (slots.isEmpty()) {
            return "En este momento no veo horarios libres dentro de la agenda configurada.";
        }

        String weeklyRanges = formatAvailabilityRanges(slots);
        if (hasText(weeklyRanges)) {
            return "Te puedo ofrecer " + weeklyRanges + ".";
        }
        return "Te puedo ofrecer " + formatSlots(slots.subList(0, Math.min(slots.size(), 3))) + ".";
    }

    private String buildDateSpecificReply(AvailabilityRequest request) {
        List<AvailabilitySlot> slots = calendarAvailabilityService.findAvailableSlots(request, FETCH_LIMIT);
        String dayLabel = formatDayLabel(request.requestedDate(), slots);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-AR"));

        List<AvailabilitySlot> daySlots = slots.stream()
                .filter(slot -> slot.start().toLocalDate().equals(request.requestedDate()))
                .sorted(Comparator.comparing(AvailabilitySlot::start))
                .toList();

        if (daySlots.isEmpty()) {
            if (request.hasTime()) {
                return "Para el " + dayLabel + " a las " + request.requestedTime().format(timeFormatter) + " no veo disponibilidad.";
            }
            return "Para el " + dayLabel + " no veo horarios disponibles.";
        }

        String dayRanges = formatAvailabilityRanges(daySlots);
        if (request.hasTime()) {
            String punctualMessage = isRequestedTimeAvailable(request.requestedTime(), daySlots)
                    ? "Para el " + dayLabel + " a las " + request.requestedTime().format(timeFormatter) + " tengo disponibilidad."
                    : "Para el " + dayLabel + " a las " + request.requestedTime().format(timeFormatter) + " no tengo disponibilidad puntual.";
            return hasText(dayRanges) ? punctualMessage + " Ese dia podria ofrecerte " + dayRanges + "." : punctualMessage;
        }

        return hasText(dayRanges)
                ? "Para el " + dayLabel + " podria ofrecerte " + dayRanges + "."
                : "Para el " + dayLabel + " no veo horarios disponibles.";
    }

    private boolean isRequestedTimeAvailable(LocalTime requestedTime, List<AvailabilitySlot> daySlots) {
        if (requestedTime == null) {
            return false;
        }
        for (AvailabilitySlot slot : daySlots) {
            LocalTime slotStart = slot.start().toLocalTime();
            LocalTime slotEnd = slot.end().toLocalTime();
            if (!requestedTime.isBefore(slotStart) && requestedTime.isBefore(slotEnd)) {
                return true;
            }
        }
        return false;
    }

    private String formatAvailabilityRanges(List<AvailabilitySlot> slots) {
        List<AvailabilityWindow> windows = buildAvailabilityWindows(slots);
        if (windows.isEmpty()) {
            return "";
        }

        Locale locale = Locale.forLanguageTag("es-AR");
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE dd/MM", locale);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale);
        Map<LocalDate, List<AvailabilityWindow>> windowsByDay = new LinkedHashMap<>();
        for (AvailabilityWindow window : windows) {
            windowsByDay.computeIfAbsent(window.day(), ignored -> new ArrayList<>()).add(window);
        }

        List<String> dayLabels = new ArrayList<>();
        ZoneId zone = slots.getFirst().start().getZone();
        for (Map.Entry<LocalDate, List<AvailabilityWindow>> entry : windowsByDay.entrySet()) {
            String dayLabel = entry.getKey().atStartOfDay(zone).format(dayFormatter).toLowerCase(Locale.ROOT);
            List<String> ranges = entry.getValue().stream()
                    .map(window -> "de " + window.start().format(timeFormatter) + " a " + window.end().format(timeFormatter))
                    .toList();
            dayLabels.add(dayLabel + " " + joinReadable(ranges));
        }
        return joinReadable(dayLabels);
    }

    private List<AvailabilityWindow> buildAvailabilityWindows(List<AvailabilitySlot> slots) {
        if (slots.isEmpty()) {
            return List.of();
        }
        List<AvailabilityWindow> windows = new ArrayList<>();
        AvailabilitySlot first = slots.getFirst();
        LocalDate currentDay = first.start().toLocalDate();
        LocalTime currentStart = first.start().toLocalTime();
        LocalTime currentEnd = first.end().toLocalTime();

        for (int i = 1; i < slots.size(); i++) {
            AvailabilitySlot slot = slots.get(i);
            LocalDate slotDay = slot.start().toLocalDate();
            LocalTime slotStart = slot.start().toLocalTime();
            LocalTime slotEnd = slot.end().toLocalTime();
            if (slotDay.equals(currentDay) && slotStart.equals(currentEnd)) {
                currentEnd = slotEnd;
            } else {
                windows.add(new AvailabilityWindow(currentDay, currentStart, currentEnd));
                currentDay = slotDay;
                currentStart = slotStart;
                currentEnd = slotEnd;
            }
        }
        windows.add(new AvailabilityWindow(currentDay, currentStart, currentEnd));
        return windows;
    }

    private String formatSlots(List<AvailabilitySlot> slots) {
        Locale locale = Locale.forLanguageTag("es-AR");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", locale);
        return joinReadable(slots.stream()
                .map(slot -> slot.start().format(formatter).toLowerCase(Locale.ROOT))
                .toList());
    }

    private String formatDayLabel(LocalDate day, List<AvailabilitySlot> slots) {
        ZoneId zone = slots.isEmpty() ? resolveCalendarZoneId() : slots.getFirst().start().getZone();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.forLanguageTag("es-AR"));
        return day.atStartOfDay(zone).format(formatter).toLowerCase(Locale.ROOT);
    }

    private ZoneId resolveCalendarZoneId() {
        String configured = properties.getCalendar() == null ? null : properties.getCalendar().getTimeZone();
        String value = configured == null || configured.isBlank() ? DEFAULT_ZONE : configured.trim();
        try {
            return ZoneId.of(value);
        } catch (Exception ignored) {
            return ZoneId.of(DEFAULT_ZONE);
        }
    }

    private String joinReadable(List<String> items) {
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.getFirst();
        if (items.size() == 2) return items.get(0) + " y " + items.get(1);
        return String.join(", ", items.subList(0, items.size() - 1)) + " y " + items.getLast();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record AvailabilityWindow(LocalDate day, LocalTime start, LocalTime end) {
    }
}
