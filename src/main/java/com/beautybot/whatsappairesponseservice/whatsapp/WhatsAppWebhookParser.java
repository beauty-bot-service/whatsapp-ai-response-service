package com.beautybot.whatsappairesponseservice.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WhatsAppWebhookParser {

    public List<WhatsAppInboundMessage> extractInboundMessages(JsonNode payload) {
        List<WhatsAppInboundMessage> inboundMessages = new ArrayList<>();
        if (payload == null || !payload.isObject()) {
            return inboundMessages;
        }

        JsonNode entries = payload.path("entry");
        if (!entries.isArray()) {
            return inboundMessages;
        }

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) {
                continue;
            }

            for (JsonNode change : changes) {
                JsonNode value = change.path("value");
                JsonNode messages = value.path("messages");
                if (!messages.isArray()) {
                    continue;
                }

                for (JsonNode message : messages) {
                    String from = text(message, "from");
                    String messageId = text(message, "id");
                    String body = extractMessageBody(message);
                    if (isBlank(from) || isBlank(messageId) || isBlank(body)) {
                        continue;
                    }
                    inboundMessages.add(new WhatsAppInboundMessage(messageId, from.trim(), body.trim()));
                }
            }
        }

        return inboundMessages;
    }

    private String extractMessageBody(JsonNode message) {
        String type = text(message, "type");
        if (isBlank(type)) {
            return null;
        }

        if ("text".equals(type)) {
            return text(message.path("text"), "body");
        }
        if ("button".equals(type)) {
            return text(message.path("button"), "text");
        }
        if ("interactive".equals(type)) {
            JsonNode interactive = message.path("interactive");
            String buttonReplyTitle = text(interactive.path("button_reply"), "title");
            if (!isBlank(buttonReplyTitle)) {
                return buttonReplyTitle;
            }
            return text(interactive.path("list_reply"), "title");
        }

        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return isBlank(text) ? null : text;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


