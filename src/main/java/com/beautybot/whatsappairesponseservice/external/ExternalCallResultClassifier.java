package com.beautybot.whatsappairesponseservice.external;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ExternalCallResultClassifier {

    public String classify(Exception exception) {
        if (exception instanceof ResourceAccessException) {
            return "timeout_or_connectivity";
        }
        if (exception instanceof HttpClientErrorException.TooManyRequests) {
            return "rate_limited";
        }
        if (exception instanceof HttpClientErrorException.Unauthorized) {
            return "unauthorized";
        }
        if (exception instanceof HttpClientErrorException.Forbidden) {
            return "forbidden";
        }
        if (exception instanceof HttpServerErrorException) {
            return "server_error";
        }
        if (exception instanceof RestClientResponseException restException) {
            HttpStatusCode statusCode = restException.getStatusCode();
            if (statusCode.value() == 429) {
                return "rate_limited";
            }
            if (statusCode.is4xxClientError()) {
                return "client_error_" + statusCode.value();
            }
            if (statusCode.is5xxServerError()) {
                return "server_error_" + statusCode.value();
            }
        }
        return "failed";
    }
}
