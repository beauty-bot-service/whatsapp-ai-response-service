package com.beautybot.whatsappairesponseservice.application;

import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatResult;

public interface ChatService {

    ChatResult handleMessage(ChatMessage request);
}


