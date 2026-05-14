package com.beautybot.whatsappairesponseservice.external;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalCallResultClassifierTest {

    private final ExternalCallResultClassifier classifier = new ExternalCallResultClassifier();

    @Test
    void classifiesRateLimit() {
        assertThat(classifier.classify(HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                null,
                null
        ))).isEqualTo("rate_limited");
    }

    @Test
    void classifiesTimeoutOrConnectivity() {
        assertThat(classifier.classify(new ResourceAccessException("timeout")))
                .isEqualTo("timeout_or_connectivity");
    }
}
