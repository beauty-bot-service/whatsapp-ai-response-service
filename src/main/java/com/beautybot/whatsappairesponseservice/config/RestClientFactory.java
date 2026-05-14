package com.beautybot.whatsappairesponseservice.config;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientFactory {

    private static final int DEFAULT_TIMEOUT_SECONDS = 12;

    public RestClient openAiClient(BeautyBotProperties.Ai ai) {
        return RestClient.builder()
                .requestFactory(requestFactory(resolveTimeoutSeconds(ai == null ? 0 : ai.getTimeoutSeconds())))
                .baseUrl(ai.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + ai.getApiKey())
                .build();
    }

    public RestClient whatsappClient(BeautyBotProperties.Whatsapp whatsapp) {
        return RestClient.builder()
                .requestFactory(requestFactory(DEFAULT_TIMEOUT_SECONDS))
                .baseUrl(whatsapp.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + whatsapp.getAccessToken())
                .build();
    }

    public RestClient bearerClient(String baseUrl, String accessToken, int timeoutSeconds) {
        return RestClient.builder()
                .requestFactory(requestFactory(resolveTimeoutSeconds(timeoutSeconds)))
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        int timeoutMillis = resolveTimeoutSeconds(timeoutSeconds) * 1000;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return requestFactory;
    }

    private int resolveTimeoutSeconds(int timeoutSeconds) {
        return timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }
}
