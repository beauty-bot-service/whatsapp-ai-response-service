package com.beautybot.whatsappairesponseservice.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiResponseParser parser = new OpenAiResponseParser();

    @Test
    void extractsFromResponsesOutputContentTextWhenOutputTextIsMissing() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "id": "resp_123",
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"intent\\":\\"GREETING\\"}"
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThat(parser.extractText(response)).contains("{\"intent\":\"GREETING\"}");
    }

    @Test
    void extractsFromDirectOutputTextWhenPresent() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "output_text": "{\\"intent\\":\\"LOCATION_QUESTION\\"}"
                }
                """);

        assertThat(parser.extractText(response)).contains("{\"intent\":\"LOCATION_QUESTION\"}");
    }

    @Test
    void extractsFromChatCompletionsChoiceMessageContent() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"intent\\":\\"PRICE_QUESTION\\"}"
                      }
                    }
                  ]
                }
                """);

        assertThat(parser.extractText(response)).contains("{\"intent\":\"PRICE_QUESTION\"}");
    }
}

