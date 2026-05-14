package com.beautybot.whatsappairesponseservice.calendar;

import java.time.ZonedDateTime;

public record AvailabilitySlot(
        ZonedDateTime start,
        ZonedDateTime end
) {
}
