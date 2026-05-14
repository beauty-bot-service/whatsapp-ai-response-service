package com.beautybot.whatsappairesponseservice.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenAiResponseParser {

    /**
     * La Responses API devuelve el texto dentro de output[].content[].text.
     * Este parser es intencionalmente tolerante por si el proveedor compatible
     * devuelve output_text o una forma parecida.
     */
    public Optional<String> extractText(JsonNode response) {
        if (response == null || response.isNull()) {
            return Optional.empty();
        }

        Optional<String> fromOutput = extractFromResponsesOutput(response.get("output"));
        if (fromOutput.isPresent()) {
            return fromOutput;
        }

        JsonNode directOutputText = response.get("output_text");
        Optional<String> fromOutputText = coerceText(directOutputText);
        if (fromOutputText.isPresent()) {
            return fromOutputText;
        }

        Optional<String> fromChatCompletions = extractFromChatCompletions(response.get("choices"));
        if (fromChatCompletions.isPresent()) {
            return fromChatCompletions;
        }

        return findFirstTextField(response);
    }

    private Optional<String> extractFromResponsesOutput(JsonNode output) {
        if (output == null || output.isNull() || !output.isArray()) {
            return Optional.empty();
        }

        for (JsonNode item : output) {
            Optional<String> itemText = coerceText(item.get("text"));
            if (itemText.isPresent()) {
                return itemText;
            }

            JsonNode content = item.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode contentItem : content) {
                    Optional<String> contentText = coerceText(contentItem.get("text"));
                    if (contentText.isPresent()) {
                        return contentText;
                    }
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> extractFromChatCompletions(JsonNode choices) {
        if (choices == null || choices.isNull() || !choices.isArray()) {
            return Optional.empty();
        }

        for (JsonNode choice : choices) {
            JsonNode message = choice.get("message");
            if (message == null || message.isNull()) {
                continue;
            }

            Optional<String> content = coerceText(message.get("content"));
            if (content.isPresent()) {
                return content;
            }
        }

        return Optional.empty();
    }

    private Optional<String> coerceText(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isTextual()) {
            return Optional.of(node.asText());
        }

        if (node.isObject()) {
            JsonNode value = node.get("value");
            if (value != null && value.isTextual()) {
                return Optional.of(value.asText());
            }

            JsonNode text = node.get("text");
            if (text != null) {
                Optional<String> nestedText = coerceText(text);
                if (nestedText.isPresent()) {
                    return nestedText;
                }
            }

            return Optional.empty();
        }

        if (node.isArray()) {
            List<String> chunks = new ArrayList<>();
            for (JsonNode item : node) {
                Optional<String> piece = coerceText(item);
                piece.ifPresent(chunks::add);
            }
            if (!chunks.isEmpty()) {
                return Optional.of(String.join("", chunks));
            }
        }

        return Optional.empty();
    }

    private Optional<String> findFirstTextField(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isObject()) {
            JsonNode text = node.get("text");
            if (text != null && text.isTextual()) {
                return Optional.of(text.asText());
            }

            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Optional<String> found = findFirstTextField(fields.next().getValue());
                if (found.isPresent()) {
                    return found;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<String> found = findFirstTextField(child);
                if (found.isPresent()) {
                    return found;
                }
            }
        }

        return Optional.empty();
    }
}
