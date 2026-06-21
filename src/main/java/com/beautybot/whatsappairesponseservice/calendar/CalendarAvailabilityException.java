package com.beautybot.whatsappairesponseservice.calendar;

public class CalendarAvailabilityException extends RuntimeException {

    public CalendarAvailabilityException(String message) {
        super(message);
    }

    public CalendarAvailabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
