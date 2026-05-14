package com.beautybot.whatsappairesponseservice.calendar;

import java.util.List;

public interface CalendarAvailabilityService {

    List<AvailabilitySlot> findNextAvailableSlots(int maxSlots);

    default List<AvailabilitySlot> findAvailableSlots(AvailabilityRequest request, int maxSlots) {
        return findNextAvailableSlots(maxSlots);
    }

    boolean isConfigured();
}
