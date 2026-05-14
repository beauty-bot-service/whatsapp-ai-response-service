package com.beautybot.whatsappairesponseservice.outbound.state;

public enum OutboundMessageStatus {
    // Legacy values kept for backward compatibility with historical rows.
    PENDING,
    DISPATCHING,
    SENT,
    FAILED,
    CANCELLED
}
