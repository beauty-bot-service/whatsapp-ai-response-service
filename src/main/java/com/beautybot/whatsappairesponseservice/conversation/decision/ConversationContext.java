package com.beautybot.whatsappairesponseservice.conversation.decision;

import com.beautybot.whatsappairesponseservice.calendar.AvailabilityRequest;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ConversationContext {
    private ClinicContext clinic;
    private BotCapabilities botCapabilities;
    private ConversationSession currentSession;
    private ChatMessage currentMessage;
    private String lastBotMessage;
    private List<RecentConversationMessage> recentMessages;
    private Map<ConversationState, String> allowedStates;
    private List<Intent> allowedIntents;
    private List<String> requiredLeadFields;
    private AvailabilityRequest availabilityRequest;
    private List<String> availabilitySuggestions;
    private List<String> mandatoryRules;
    private List<RequiredField> allowedWaitingFields;
}
