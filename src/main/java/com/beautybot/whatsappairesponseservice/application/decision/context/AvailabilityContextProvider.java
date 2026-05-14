package com.beautybot.whatsappairesponseservice.application.decision.context;

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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AvailabilityContextProvider {

    private static final int LOOKAHEAD_DAYS = 7;
    private static final int FETCH_LIMIT = 240;
    private static final String DEFAULT_ZONE = "America/Argentina/Buenos_Aires";

    private final BeautyBotProperties properties;
    private final CalendarAvailabilityService calendarAvailabilityService;
    private final AvailabilityRequestParser availabilityRequestParser;

    public AvailabilityRequest parseRequest(String rawMessage) {
        return availabilityRequestParser.parse(rawMessage, resolveCalendarZoneId());
    }

    public AvailabilityRequest nullableRequest(AvailabilityRequest request) {
        return request != null && (request.hasDate() || request.hasTime()) ? request : null;
    }

    public List<String> buildSuggestions(AvailabilityRequest request) {
        if (!calendarAvailabilityService.isConfigured()) {
            return List.of();
        }
        if (request != null && request.hasDate()) {
            return buildDateSpecificSuggestions(request, calendarAvailabilityService.findAvailableSlots(request, FETCH_LIMIT));
        }
        List<AvailabilitySlot> slots = calendarAvailabilityService.findNextAvailableSlots(FETCH_LIMIT);
        if (slots.isEmpty()) {
            return List.of();
        }
        List<AvailabilitySlot> weeklySlots = filterUpcomingWeekSlots(slots);
        return weeklySlots.isEmpty() ? List.of() : buildDayRanges(weeklySlots);
    }

    private List<String> buildDateSpecificSuggestions(AvailabilityRequest request, List<AvailabilitySlot> slots) {
        Locale locale = Locale.forLanguageTag("es-AR");
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE dd/MM", locale);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale);
        ZoneId zone = slots.isEmpty() ? resolveCalendarZoneId() : slots.getFirst().start().getZone();
        String dayLabel = request.requestedDate().atStartOfDay(zone).format(dayFormatter).toLowerCase(Locale.ROOT);

        List<AvailabilitySlot> daySlots = slots.stream()
                .filter(slot -> slot.start().toLocalDate().equals(request.requestedDate()))
                .sorted(Comparator.comparing(AvailabilitySlot::start))
                .toList();

        if (daySlots.isEmpty()) {
            if (request.hasTime()) {
                return List.of(
                        "consulta puntual: " + dayLabel + " " + request.requestedTime().format(timeFormatter),
                        "disponibilidad puntual: no disponible",
                        dayLabel + " sin horarios disponibles"
                );
            }
            return List.of(dayLabel + " sin horarios disponibles");
        }

        List<String> lines = new ArrayList<>();
        if (request.hasTime()) {
            lines.add("consulta puntual: " + dayLabel + " " + request.requestedTime().format(timeFormatter));
            lines.add("disponibilidad puntual: " + (isRequestedTimeAvailable(request.requestedTime(), daySlots) ? "disponible" : "no disponible"));
        }
        lines.addAll(buildDayRanges(daySlots));
        return lines;
    }

    private List<AvailabilitySlot> filterUpcomingWeekSlots(List<AvailabilitySlot> slots) {
        ZoneId zone = slots.getFirst().start().getZone();
        LocalDate startDate = ZonedDateTime.now(zone).toLocalDate();
        LocalDate endDate = startDate.plusDays(LOOKAHEAD_DAYS - 1L);
        return slots.stream()
                .filter(slot -> {
                    LocalDate day = slot.start().toLocalDate();
                    return !day.isBefore(startDate) && !day.isAfter(endDate);
                })
                .sorted(Comparator.comparing(AvailabilitySlot::start))
                .toList();
    }

    private List<String> buildDayRanges(List<AvailabilitySlot> slots) {
        List<AvailabilityWindow> windows = buildAvailabilityWindows(slots);
        if (windows.isEmpty()) {
            return List.of();
        }
        Locale locale = Locale.forLanguageTag("es-AR");
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEE dd/MM", locale);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale);
        Map<LocalDate, List<AvailabilityWindow>> windowsByDay = new LinkedHashMap<>();
        for (AvailabilityWindow window : windows) {
            windowsByDay.computeIfAbsent(window.day(), ignored -> new ArrayList<>()).add(window);
        }
        List<String> lines = new ArrayList<>();
        ZoneId zone = slots.getFirst().start().getZone();
        for (Map.Entry<LocalDate, List<AvailabilityWindow>> entry : windowsByDay.entrySet()) {
            String dayLabel = entry.getKey().atStartOfDay(zone).format(dayFormatter).toLowerCase(Locale.ROOT);
            List<String> ranges = entry.getValue().stream()
                    .map(window -> "de " + window.start().format(timeFormatter) + " a " + window.end().format(timeFormatter))
                    .toList();
            lines.add(dayLabel + " " + joinReadable(ranges));
        }
        return lines;
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

    private record AvailabilityWindow(LocalDate day, LocalTime start, LocalTime end) {
    }
}
