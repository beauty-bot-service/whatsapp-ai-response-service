package com.beautybot.whatsappairesponseservice.conversation.model;

import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.ContactPreference;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationSession {

    private Long id;
    private String phoneNumber;
    private ConversationState state;
    private String customerName;
    private String treatmentInterest;
    private Boolean firstTime;
    private String preferredTime;
    private ContactPreference contactPreference;
    private RequiredField waitingForField;
    private Boolean requiresHuman;
    private String summaryForHuman;
    private LocalDateTime humanNotifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


