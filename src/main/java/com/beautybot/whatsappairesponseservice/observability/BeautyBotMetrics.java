package com.beautybot.whatsappairesponseservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BeautyBotMetrics {

    private final MeterRegistry meterRegistry;

    public BeautyBotMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void inboundMessage(String channel, String result) {
        counter("beauty_bot_inbound_messages_total", "channel", safe(channel), "result", safe(result)).increment();
    }

    public void outboundMessage(String channel, String result) {
        counter("beauty_bot_outbound_messages_total", "channel", safe(channel), "result", safe(result)).increment();
    }

    public void aiDecision(String result) {
        counter("beauty_bot_ai_decisions_total", "result", safe(result)).increment();
    }

    public void ruleBasedFallback(String reason) {
        counter("beauty_bot_rule_based_fallback_total", "reason", safe(reason)).increment();
    }

    public void handoff(String result) {
        counter("beauty_bot_handoff_total", "result", safe(result)).increment();
    }

    public void outbox(String status) {
        counter("beauty_bot_outbox_events_total", "status", safe(status)).increment();
    }

    public void externalCall(String source, String result) {
        counter("beauty_bot_external_call_total", "source", safe(source), "result", safe(result)).increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(meterRegistry);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase();
    }
}
