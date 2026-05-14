package com.beautybot.whatsappairesponseservice.calendar;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.config.RestClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarAvailabilityService implements CalendarAvailabilityService {

    private static final String GOOGLE_CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.readonly";
    private static final String GOOGLE_CALENDAR_BASE_URL = "https://www.googleapis.com/calendar/v3";
    private static final int MAX_SLOT_QUERY_LIMIT = 500;

    private final BeautyBotProperties properties;
    private final RestClientFactory restClientFactory;

    @Override
    public List<AvailabilitySlot> findNextAvailableSlots(int maxSlots) {
        if (!isConfigured()) {
            return List.of();
        }

        CalendarQueryConfig config = resolveQueryConfig();
        if (config == null) {
            return List.of();
        }

        ZonedDateTime searchStart = ZonedDateTime.now(config.zoneId()).plusMinutes(config.minimumNoticeMinutes());
        ZonedDateTime searchEnd = searchStart.plusDays(config.lookaheadDays());

        List<BusyInterval> busyIntervals = fetchBusyIntervals(searchStart, searchEnd, config.zoneId());
        return computeAvailableSlots(
                searchStart,
                searchEnd,
                config.workingStart(),
                config.workingEnd(),
                config.workingDays(),
                config.slotDurationMinutes(),
                resolveSuggestionsLimit(maxSlots, config.defaultSuggestions()),
                busyIntervals
        );
    }

    @Override
    public List<AvailabilitySlot> findAvailableSlots(AvailabilityRequest request, int maxSlots) {
        if (request == null || !request.hasDate()) {
            return findNextAvailableSlots(maxSlots);
        }

        if (!isConfigured()) {
            return List.of();
        }

        CalendarQueryConfig config = resolveQueryConfig();
        if (config == null) {
            return List.of();
        }

        LocalDate requestedDate = request.requestedDate();
        ZonedDateTime nowWithNotice = ZonedDateTime.now(config.zoneId()).plusMinutes(config.minimumNoticeMinutes());
        LocalDate latestDate = nowWithNotice.toLocalDate().plusDays(config.lookaheadDays() - 1L);
        if (requestedDate.isBefore(nowWithNotice.toLocalDate()) || requestedDate.isAfter(latestDate)) {
            return List.of();
        }
        if (!config.workingDays().contains(requestedDate.getDayOfWeek())) {
            return List.of();
        }

        ZonedDateTime dayStart = ZonedDateTime.of(requestedDate, config.workingStart(), config.zoneId());
        ZonedDateTime dayEnd = ZonedDateTime.of(requestedDate, config.workingEnd(), config.zoneId());
        if (!dayEnd.isAfter(dayStart)) {
            return List.of();
        }

        ZonedDateTime searchStart = dayStart;
        if (requestedDate.equals(nowWithNotice.toLocalDate()) && nowWithNotice.isAfter(dayStart)) {
            searchStart = alignToSlotBoundary(dayStart, nowWithNotice, config.slotDurationMinutes());
        }
        if (!dayEnd.isAfter(searchStart)) {
            return List.of();
        }

        List<BusyInterval> busyIntervals = fetchBusyIntervals(searchStart, dayEnd, config.zoneId());
        return computeAvailableSlots(
                searchStart,
                dayEnd,
                config.workingStart(),
                config.workingEnd(),
                config.workingDays(),
                config.slotDurationMinutes(),
                resolveSuggestionsLimit(maxSlots, config.defaultSuggestions()),
                busyIntervals
        );
    }

    @Override
    public boolean isConfigured() {
        BeautyBotProperties.CalendarConfig calendar = properties.getCalendar();
        if (calendar == null || !calendar.isEnabled()) {
            return false;
        }

        BeautyBotProperties.BotCapabilitiesConfig capabilities = properties.getBotCapabilities();
        if (capabilities == null || !capabilities.isCanCheckAvailability()) {
            return false;
        }

        BeautyBotProperties.CalendarConfig.Google google = calendar.getGoogle();
        if (google == null) {
            return false;
        }

        boolean hasCredentials = hasText(google.getServiceAccountJsonBase64()) || hasText(google.getServiceAccountJson());
        return hasCredentials && hasText(google.getCalendarId());
    }

    private CalendarQueryConfig resolveQueryConfig() {
        BeautyBotProperties.CalendarConfig calendar = properties.getCalendar();
        if (calendar == null || calendar.getWorkingHours() == null) {
            log.warn("Calendar working-hours configuration is missing.");
            return null;
        }

        ZoneId zoneId = resolveZoneId(calendar.getTimeZone());
        if (zoneId == null) {
            return null;
        }

        LocalTime workingStart = resolveLocalTime(calendar.getWorkingHours().getStart(), "09:00");
        LocalTime workingEnd = resolveLocalTime(calendar.getWorkingHours().getEnd(), "18:00");
        if (!workingEnd.isAfter(workingStart)) {
            log.warn("Calendar working-hours.end must be after working-hours.start.");
            return null;
        }

        Set<DayOfWeek> workingDays = resolveWorkingDays(calendar.getWorkingHours().getDays());
        if (workingDays.isEmpty()) {
            log.warn("Calendar working-hours.days has no valid values.");
            return null;
        }

        return new CalendarQueryConfig(
                zoneId,
                workingStart,
                workingEnd,
                workingDays,
                positiveOrDefault(calendar.getSlotDurationMinutes(), 30),
                positiveOrDefault(calendar.getLookaheadDays(), 14),
                nonNegativeOrDefault(calendar.getMinimumNoticeMinutes(), 120),
                positiveOrDefault(calendar.getMaxSuggestions(), 3)
        );
    }

    private List<BusyInterval> fetchBusyIntervals(ZonedDateTime searchStart, ZonedDateTime searchEnd, ZoneId zoneId) {
        String calendarId = properties.getCalendar().getGoogle().getCalendarId();
        String accessToken = fetchAccessToken();
        if (!hasText(accessToken)) {
            return List.of();
        }

        try {
            RestClient restClient = restClientFactory.bearerClient(GOOGLE_CALENDAR_BASE_URL, accessToken, 12);

            Map<String, Object> requestBody = Map.of(
                    "timeMin", searchStart.toInstant().toString(),
                    "timeMax", searchEnd.toInstant().toString(),
                    "timeZone", zoneId.getId(),
                    "items", List.of(Map.of("id", calendarId))
            );

            JsonNode response = restClient.post()
                    .uri("/freeBusy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode busyArray = response == null
                    ? null
                    : response.path("calendars").path(calendarId).path("busy");
            if (busyArray == null || !busyArray.isArray()) {
                return List.of();
            }

            List<BusyInterval> intervals = new ArrayList<>();
            for (JsonNode item : busyArray) {
                BusyInterval interval = toBusyInterval(item);
                if (interval != null) {
                    intervals.add(interval);
                }
            }

            intervals.sort(Comparator.comparing(BusyInterval::start));
            return mergeOverlappingBusyIntervals(intervals);
        } catch (Exception e) {
            log.warn("Google Calendar freeBusy query failed. Cause: {}", e.getMessage());
            return List.of();
        }
    }

    private BusyInterval toBusyInterval(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        String startText = node.path("start").asText(null);
        String endText = node.path("end").asText(null);
        if (!hasText(startText) || !hasText(endText)) {
            return null;
        }

        try {
            Instant start = OffsetDateTime.parse(startText).toInstant();
            Instant end = OffsetDateTime.parse(endText).toInstant();
            if (!end.isAfter(start)) {
                return null;
            }
            return new BusyInterval(start, end);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<BusyInterval> mergeOverlappingBusyIntervals(List<BusyInterval> intervals) {
        if (intervals.isEmpty()) {
            return List.of();
        }

        List<BusyInterval> merged = new ArrayList<>();
        BusyInterval current = intervals.getFirst();
        for (int i = 1; i < intervals.size(); i++) {
            BusyInterval next = intervals.get(i);
            if (!next.start().isAfter(current.end())) {
                Instant mergedEnd = next.end().isAfter(current.end()) ? next.end() : current.end();
                current = new BusyInterval(current.start(), mergedEnd);
                continue;
            }
            merged.add(current);
            current = next;
        }
        merged.add(current);
        return merged;
    }

    private List<AvailabilitySlot> computeAvailableSlots(
            ZonedDateTime searchStart,
            ZonedDateTime searchEnd,
            LocalTime workingStart,
            LocalTime workingEnd,
            Set<DayOfWeek> workingDays,
            int slotDurationMinutes,
            int suggestionsLimit,
            List<BusyInterval> busyIntervals
    ) {
        List<AvailabilitySlot> slots = new ArrayList<>();
        LocalDate endDate = searchEnd.toLocalDate();

        for (LocalDate day = searchStart.toLocalDate(); !day.isAfter(endDate) && slots.size() < suggestionsLimit; day = day.plusDays(1)) {
            if (!workingDays.contains(day.getDayOfWeek())) {
                continue;
            }

            ZonedDateTime dayStart = ZonedDateTime.of(day, workingStart, searchStart.getZone());
            ZonedDateTime dayEnd = ZonedDateTime.of(day, workingEnd, searchStart.getZone());
            if (!dayEnd.isAfter(dayStart)) {
                continue;
            }

            ZonedDateTime slotStart = dayStart;
            if (day.equals(searchStart.toLocalDate()) && searchStart.isAfter(dayStart)) {
                slotStart = alignToSlotBoundary(dayStart, searchStart, slotDurationMinutes);
            }

            while (slots.size() < suggestionsLimit) {
                ZonedDateTime slotEnd = slotStart.plusMinutes(slotDurationMinutes);
                if (slotEnd.isAfter(dayEnd) || slotStart.isAfter(searchEnd)) {
                    break;
                }

                if (!overlapsBusy(slotStart.toInstant(), slotEnd.toInstant(), busyIntervals)) {
                    slots.add(new AvailabilitySlot(slotStart, slotEnd));
                }
                slotStart = slotStart.plusMinutes(slotDurationMinutes);
            }
        }

        return slots;
    }

    private ZonedDateTime alignToSlotBoundary(ZonedDateTime dayStart, ZonedDateTime value, int slotDurationMinutes) {
        long minutesFromStart = Duration.between(dayStart, value).toMinutes();
        if (minutesFromStart <= 0) {
            return dayStart;
        }
        long remainder = minutesFromStart % slotDurationMinutes;
        long delta = remainder == 0 ? 0 : (slotDurationMinutes - remainder);
        return value.plusMinutes(delta)
                .withSecond(0)
                .withNano(0);
    }

    private boolean overlapsBusy(Instant slotStart, Instant slotEnd, List<BusyInterval> busyIntervals) {
        for (BusyInterval busy : busyIntervals) {
            if (!slotStart.isBefore(busy.end())) {
                continue;
            }
            if (!slotEnd.isAfter(busy.start())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private String fetchAccessToken() {
        try {
            GoogleCredentials credentials = buildCredentials();
            if (credentials == null) {
                return null;
            }
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token == null || token.getTokenValue() == null) {
                credentials.refresh();
                token = credentials.getAccessToken();
            }
            return token == null ? null : token.getTokenValue();
        } catch (Exception e) {
            log.warn("Failed to obtain Google Calendar access token. Cause: {}", e.getMessage());
            return null;
        }
    }

    private GoogleCredentials buildCredentials() throws Exception {
        byte[] jsonBytes = resolveServiceAccountBytes();
        if (jsonBytes == null || jsonBytes.length == 0) {
            return null;
        }

        return GoogleCredentials.fromStream(new ByteArrayInputStream(jsonBytes))
                .createScoped(Collections.singleton(GOOGLE_CALENDAR_SCOPE));
    }

    private byte[] resolveServiceAccountBytes() {
        BeautyBotProperties.CalendarConfig.Google google = properties.getCalendar().getGoogle();
        if (google == null) {
            return null;
        }

        if (hasText(google.getServiceAccountJsonBase64())) {
            try {
                return Base64.getDecoder().decode(google.getServiceAccountJsonBase64().trim());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid Base64 for beauty-bot.calendar.google.service-account-json-base64.");
                return null;
            }
        }

        if (hasText(google.getServiceAccountJson())) {
            return google.getServiceAccountJson().getBytes(StandardCharsets.UTF_8);
        }

        return null;
    }

    private ZoneId resolveZoneId(String zoneId) {
        String value = hasText(zoneId) ? zoneId.trim() : "America/Argentina/Buenos_Aires";
        try {
            return ZoneId.of(value);
        } catch (Exception e) {
            log.warn("Invalid calendar timezone '{}'.", value);
            return null;
        }
    }

    private LocalTime resolveLocalTime(String value, String fallback) {
        String selected = hasText(value) ? value.trim() : fallback;
        try {
            return LocalTime.parse(selected);
        } catch (Exception e) {
            return LocalTime.parse(fallback);
        }
    }

    private Set<DayOfWeek> resolveWorkingDays(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        }

        Set<DayOfWeek> days = new LinkedHashSet<>();
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            try {
                days.add(DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (Exception ignored) {
                // Ignora valores invalidos para no romper la carga de configuracion.
            }
        }
        return days;
    }

    private int resolveSuggestionsLimit(int requestedLimit, int defaultLimit) {
        int safeDefault = positiveOrDefault(defaultLimit, 3);
        if (requestedLimit <= 0) {
            return safeDefault;
        }
        return Math.min(requestedLimit, MAX_SLOT_QUERY_LIMIT);
    }

    private int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private int nonNegativeOrDefault(int value, int fallback) {
        return value >= 0 ? value : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CalendarQueryConfig(
            ZoneId zoneId,
            LocalTime workingStart,
            LocalTime workingEnd,
            Set<DayOfWeek> workingDays,
            int slotDurationMinutes,
            int lookaheadDays,
            int minimumNoticeMinutes,
            int defaultSuggestions
    ) {
    }

    private record BusyInterval(Instant start, Instant end) {
    }
}
