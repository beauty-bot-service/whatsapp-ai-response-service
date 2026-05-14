package com.beautybot.whatsappairesponseservice.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityRequestParserTest {

    private final AvailabilityRequestParser parser = new AvailabilityRequestParser();
    private final ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void parsesDateWithoutYearUsingArgentinianFormat() {
        AvailabilityRequest request = parser.parse("Tienen turno el 5/4?", zoneId);

        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
        LocalDate expected = LocalDate.of(today.getYear(), 4, 5);
        if (expected.isBefore(today)) {
            expected = expected.plusYears(1);
        }

        assertThat(request.requestedDate()).isEqualTo(expected);
        assertThat(request.requestedTime()).isNull();
    }

    @Test
    void parsesDateAndTime() {
        AvailabilityRequest request = parser.parse("Necesito turno 13/05 a las 15:30", zoneId);

        LocalDate today = ZonedDateTime.now(zoneId).toLocalDate();
        LocalDate expected = LocalDate.of(today.getYear(), 5, 13);
        if (expected.isBefore(today)) {
            expected = expected.plusYears(1);
        }

        assertThat(request.requestedDate()).isEqualTo(expected);
        assertThat(request.requestedTime()).isEqualTo(LocalTime.of(15, 30));
    }

    @Test
    void returnsEmptyRequestWhenNoDateIsPresent() {
        AvailabilityRequest request = parser.parse("Tienen disponibilidad esta semana?", zoneId);

        assertThat(request.requestedDate()).isNull();
        assertThat(request.requestedTime()).isNull();
    }
}
