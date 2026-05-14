package com.beautybot.whatsappairesponseservice.whatsapp;

public class WhatsAppInboundMessage {

    private final String messageId;
    private final String fromPhone;
    private final String textBody;

    public WhatsAppInboundMessage(String messageId, String fromPhone, String textBody) {
        this.messageId = messageId;
        this.fromPhone = fromPhone;
        this.textBody = textBody;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getFromPhone() {
        return fromPhone;
    }

    public String getTextBody() {
        return textBody;
    }
}


