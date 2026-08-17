package com.beautybot.whatsappairesponseservice.conversation.decision;

import com.beautybot.whatsappairesponseservice.calendar.AvailabilityRequest;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.promotion.PromotionSummary;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConversationContext {
    private ClinicContext clinic;
    private BotCapabilities botCapabilities;
    private ConversationSession currentSession;
    private ChatMessage currentMessage;
    private String lastBotMessage;
    private List<RecentConversationMessage> recentMessages;
    private AvailabilityRequest availabilityRequest;
    private List<String> availabilitySuggestions;
    private List<PromotionSummary> activePromotions;
    private boolean availabilityLookupFailed;
    private String availabilityFailureReason;
}
