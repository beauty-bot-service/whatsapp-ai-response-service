package com.beautybot.whatsappairesponseservice.calendar;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityRequest(
        LocalDate requestedDate,
        LocalTime requestedTime
) {
    public boolean hasDate() {
        return requestedDate != null;
    }

    public boolean hasTime() {
        return requestedTime != null;
    }
}
