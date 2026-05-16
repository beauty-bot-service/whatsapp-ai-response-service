package com.beautybot.whatsappairesponseservice.application.exception;

import java.util.Arrays;
import java.util.Objects;

public class AppException extends RuntimeException {

    private final ResponseCode responseCode;
    private final Object[] messageArgs;

    public AppException(ResponseCode responseCode, Object... messageArgs) {
        super(Objects.requireNonNull(responseCode, "responseCode is required").internalMessage(messageArgs));
        this.responseCode = responseCode;
        this.messageArgs = copyArgs(messageArgs);
    }

    public AppException(ResponseCode responseCode, Throwable cause, Object... messageArgs) {
        super(Objects.requireNonNull(responseCode, "responseCode is required").internalMessage(messageArgs), cause);
        this.responseCode = responseCode;
        this.messageArgs = copyArgs(messageArgs);
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }

    public String getUserMessage() {
        return responseCode.userMessage(messageArgs);
    }

    private Object[] copyArgs(Object[] messageArgs) {
        if (messageArgs == null || messageArgs.length == 0) {
            return new Object[0];
        }
        return Arrays.copyOf(messageArgs, messageArgs.length);
    }
}
