package com.beautybot.whatsappairesponseservice.calendar;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AvailabilityRequestParser {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
    private static final Pattern TIME_WITH_CONTEXT_PATTERN = Pattern.compile("(?i)\\b(?:a\\s*las|desde\\s*las|tipo|para\\s*las|alrededor\\s*de\\s*las)\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|hs|h)?\\b");
    private static final Pattern TIME_HHMM_PATTERN = Pattern.compile("(?i)\\b(\\d{1,2}):(\\d{2})\\s*(am|pm|hs|h)?\\b");
    private static final Pattern TIME_WITH_SUFFIX_PATTERN = Pattern.compile("(?i)\\b(\\d{1,2})\\s*(am|pm|hs|h)\\b");

    public AvailabilityRequest parse(String rawMessage, ZoneId zoneId) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return new AvailabilityRequest(null, null);
        }

        String message = rawMessage.trim();
        Matcher dateMatcher = DATE_PATTERN.matcher(message);
        if (!dateMatcher.find()) {
            return new AvailabilityRequest(null, null);
        }

        LocalDate requestedDate = parseDate(dateMatcher, zoneId);
        if (requestedDate == null) {
            return new AvailabilityRequest(null, null);
        }

        int dateStart = dateMatcher.start();
        int dateEnd = dateMatcher.end();
        LocalTime requestedTime = parseTime(message, dateStart, dateEnd);
        return new AvailabilityRequest(requestedDate, requestedTime);
    }

    private LocalDate parseDate(Matcher matcher, ZoneId zoneId) {
        int day = parseNumber(matcher.group(1));
        int month = parseNumber(matcher.group(2));
        if (day < 1 || day > 31 || month < 1 || month > 12) {
            return null;
        }

        try {
            String yearGroup = matcher.group(3);
            if (yearGroup != null && !yearGroup.isBlank()) {
                int year = parseNumber(yearGroup);
                if (yearGroup.length() == 2) {
                    year += 2000;
                }
                return LocalDate.of(year, month, day);
            }

            LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
            LocalDate candidate = LocalDate.of(today.getYear(), month, day);
            if (candidate.isBefore(today)) {
                candidate = candidate.plusYears(1);
            }
            return candidate;
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalTime parseTime(String message, int dateStart, int dateEnd) {
        LocalTime withContext = parseTimeWithPattern(TIME_WITH_CONTEXT_PATTERN, message, dateStart, dateEnd, true);
        if (withContext != null) {
            return withContext;
        }

        LocalTime hhmm = parseTimeWithPattern(TIME_HHMM_PATTERN, message, dateStart, dateEnd, false);
        if (hhmm != null) {
            return hhmm;
        }

        return parseTimeWithPattern(TIME_WITH_SUFFIX_PATTERN, message, dateStart, dateEnd, false);
    }

    private LocalTime parseTimeWithPattern(
            Pattern pattern,
            String message,
            int dateStart,
            int dateEnd,
            boolean minutesOptional
    ) {
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            if (overlapsDateToken(matcher.start(), matcher.end(), dateStart, dateEnd)) {
                continue;
            }

            int hour = parseNumber(matcher.group(1));
            int minute;
            if (minutesOptional) {
                String minuteGroup = matcher.group(2);
                minute = minuteGroup == null ? 0 : parseNumber(minuteGroup);
            } else {
                String minuteGroup = matcher.groupCount() >= 2 ? matcher.group(2) : null;
                minute = minuteGroup == null || isMarker(minuteGroup) ? 0 : parseNumber(minuteGroup);
            }

            String markerGroup = matcher.group(matcher.groupCount());
            String marker = markerGroup == null ? "" : markerGroup.toLowerCase(Locale.ROOT);
            if ("am".equals(marker) || "pm".equals(marker)) {
                if (hour < 1 || hour > 12) {
                    continue;
                }
                if ("am".equals(marker)) {
                    hour = hour == 12 ? 0 : hour;
                } else {
                    hour = hour == 12 ? 12 : hour + 12;
                }
            }

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                continue;
            }

            return LocalTime.of(hour, minute);
        }

        return null;
    }

    private boolean overlapsDateToken(int start, int end, int dateStart, int dateEnd) {
        return start < dateEnd && end > dateStart;
    }

    private int parseNumber(String value) {
        return Integer.parseInt(value.trim());
    }

    private boolean isMarker(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return "am".equals(lower) || "pm".equals(lower) || "hs".equals(lower) || "h".equals(lower);
    }
}
