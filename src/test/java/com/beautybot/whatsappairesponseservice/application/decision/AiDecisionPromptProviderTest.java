package com.beautybot.whatsappairesponseservice.application.decision;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiDecisionPromptProviderTest {

    @Test
    void loadsAndTrimsPromptOnce() {
        AiDecisionPromptProvider provider = provider("  fixed rules  \n");

        assertThat(provider.instructions()).isEqualTo("fixed rules");
        assertThat(provider.fingerprint()).isNotBlank();
    }

    @Test
    void rejectsEmptyPrompt() {
        assertThatThrownBy(() -> provider("  \n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI decision prompt must not be empty");
    }

    private AiDecisionPromptProvider provider(String content) {
        return new AiDecisionPromptProvider(
                new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)));
    }
}
