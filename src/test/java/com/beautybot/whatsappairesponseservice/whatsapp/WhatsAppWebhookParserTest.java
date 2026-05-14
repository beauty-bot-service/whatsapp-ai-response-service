package com.beautybot.whatsappairesponseservice.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppWebhookParserTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final WhatsAppWebhookParser parser = new WhatsAppWebhookParser();

    @Test
    void extractsInboundMessageWhenFromIdAndBodyArePresent() throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "entry": [
                    {
                      "changes": [
                        {
                          "value": {
                            "messages": [
                              {
                                "from": "5491112345678",
                                "id": "wamid.1",
                                "type": "text",
                                "text": {
                                  "body": "hola"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """);

        List<WhatsAppInboundMessage> messages = parser.extractInboundMessages(payload);

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().getMessageId()).isEqualTo("wamid.1");
        assertThat(messages.getFirst().getFromPhone()).isEqualTo("5491112345678");
        assertThat(messages.getFirst().getTextBody()).isEqualTo("hola");
    }

    @Test
    void ignoresInboundMessageWhenMessageIdIsMissing() throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "entry": [
                    {
                      "changes": [
                        {
                          "value": {
                            "messages": [
                              {
                                "from": "5491112345678",
                                "type": "text",
                                "text": {
                                  "body": "hola"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """);

        List<WhatsAppInboundMessage> messages = parser.extractInboundMessages(payload);

        assertThat(messages).isEmpty();
    }
}
