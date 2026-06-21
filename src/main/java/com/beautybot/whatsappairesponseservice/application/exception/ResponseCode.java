package com.beautybot.whatsappairesponseservice.application.exception;

import java.util.IllegalFormatException;
import java.util.Locale;

public enum ResponseCode {
    INVALID_CHAT_MESSAGE("INVALID_CHAT_MESSAGE", "Invalid chat message payload.", "El mensaje recibido no es valido."),
    INVALID_PHONE_NUMBER("INVALID_PHONE_NUMBER", "phoneNumber is invalid.", "El numero de telefono es invalido."),
    PHONE_NUMBER_MUST_BE_NUMERIC("PHONE_NUMBER_MUST_BE_NUMERIC", "phoneNumber must contain only digits after normalization.", "El numero de telefono debe contener solo digitos."),
    PHONE_NUMBER_LENGTH_OUT_OF_RANGE("PHONE_NUMBER_LENGTH_OUT_OF_RANGE", "phoneNumber must have between %d and %d digits.", "El numero de telefono debe tener entre %d y %d digitos."),
    INVALID_MESSAGE_TEXT("INVALID_MESSAGE_TEXT", "message is invalid.", "El mensaje no es valido."),
    MESSAGE_LENGTH_EXCEEDED("MESSAGE_LENGTH_EXCEEDED", "message exceeds max length of %d characters.", "El mensaje supera el maximo de %d caracteres."),
    INVALID_FIELD_VALUE("INVALID_FIELD_VALUE", "Field '%s' is invalid.", "El campo '%s' es invalido."),
    FIELD_LENGTH_EXCEEDED("FIELD_LENGTH_EXCEEDED", "Field '%s' exceeds max length of %d characters.", "El campo '%s' supera el maximo de %d caracteres."),
    CONVERSATION_PHONE_REQUIRED("CONVERSATION_PHONE_REQUIRED", "phoneNumber is required.", "El numero de telefono es obligatorio."),
    LEAD_REQUEST_REQUIRED("LEAD_REQUEST_REQUIRED", "request is required.", "La solicitud es obligatoria."),
    LEAD_CLINIC_ID_REQUIRED("LEAD_CLINIC_ID_REQUIRED", "clinicId is required.", "El identificador de la clinica es obligatorio."),
    LEAD_PHONE_REQUIRED("LEAD_PHONE_REQUIRED", "phoneNumber is required.", "El numero de telefono es obligatorio."),
    LEAD_NOT_FOUND("LEAD_NOT_FOUND", "Lead not found.", "No se encontro el lead solicitado."),
    LEAD_STATUS_REQUIRED("LEAD_STATUS_REQUIRED", "newStatus is required.", "El estado del lead es obligatorio."),
    LEAD_NOTE_REQUIRED("LEAD_NOTE_REQUIRED", "note is required.", "La nota es obligatoria."),
    OUTBOUND_SESSION_ID_REQUIRED("OUTBOUND_SESSION_ID_REQUIRED", "session with id is required.", "La sesion es obligatoria."),
    OUTBOUND_SESSION_PHONE_REQUIRED("OUTBOUND_SESSION_PHONE_REQUIRED", "session.phoneNumber is required.", "El telefono de la sesion es obligatorio."),
    OUTBOUND_CONTENT_REQUIRED("OUTBOUND_CONTENT_REQUIRED", "content is required.", "El contenido del mensaje es obligatorio."),
    AI_DECISION_CONFIGURATION_ERROR("AI_DECISION_CONFIGURATION_ERROR", "AI decision is enabled but OPENAI_API_KEY or decision prompt template is missing, or AI is disabled.", "No se pudo procesar la solicitud en este momento."),
    AI_DECISION_REQUEST_FAILED("AI_DECISION_REQUEST_FAILED", "AI conversation decision failed: %s.", "No se pudo procesar la solicitud en este momento."),
    WHATSAPP_SIGNATURE_VALIDATION_ERROR("WHATSAPP_SIGNATURE_VALIDATION_ERROR", "Unable to validate WhatsApp webhook signature.", "No se pudo validar la firma del webhook."),
    BAD_REQUEST("BAD_REQUEST", "Bad request.", "La solicitud es invalida.");

    private final String code;
    private final String internalMessageTemplate;
    private final String userMessageTemplate;

    ResponseCode(String code, String internalMessageTemplate, String userMessageTemplate) {
        this.code = code;
        this.internalMessageTemplate = internalMessageTemplate;
        this.userMessageTemplate = userMessageTemplate;
    }

    public String getCode() {
        return code;
    }

    public String internalMessage(Object... args) {
        return format(internalMessageTemplate, args);
    }

    public String userMessage(Object... args) {
        return format(userMessageTemplate, args);
    }

    private String format(String template, Object... args) {
        if (template == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(Locale.ROOT, template, args);
        } catch (IllegalFormatException ignored) {
            return template;
        }
    }
}
