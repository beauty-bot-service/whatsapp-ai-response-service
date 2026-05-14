package com.beautybot.whatsappairesponseservice.conversation.reply;

import org.springframework.stereotype.Component;

@Component
public class ReplyStyleNormalizer {

    public String normalize(String reply) {
        if (reply == null) {
            return null;
        }
        return reply.trim()
                .replace("¡", "")
                .replace("!", ".")
                .replace("¿", "");
    }
}
