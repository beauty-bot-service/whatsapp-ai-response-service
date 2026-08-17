package com.beautybot.whatsappairesponseservice.admin.session;

public record CsrfResponse(String token, String headerName, String parameterName) {
}
