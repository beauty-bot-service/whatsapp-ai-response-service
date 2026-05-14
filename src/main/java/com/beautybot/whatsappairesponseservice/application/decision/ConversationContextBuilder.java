package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.application.decision.context.AvailabilityContextProvider;
import com.beautybot.whatsappairesponseservice.application.decision.context.BotCapabilitiesFactory;
import com.beautybot.whatsappairesponseservice.application.decision.context.ClinicContextFactory;
import com.beautybot.whatsappairesponseservice.application.decision.context.DecisionRulesProvider;
import com.beautybot.whatsappairesponseservice.application.decision.context.RecentMessageContextProvider;
import com.beautybot.whatsappairesponseservice.calendar.AvailabilityRequest;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.RecentConversationMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationContextBuilder {

    private final ClinicContextFactory clinicContextFactory;
    private final BotCapabilitiesFactory botCapabilitiesFactory;
    private final RecentMessageContextProvider recentMessageContextProvider;
    private final AvailabilityContextProvider availabilityContextProvider;
    private final DecisionRulesProvider decisionRulesProvider;

    public ConversationContext build(ConversationSession session, ChatMessage currentMessage) {
        List<RecentConversationMessage> recentMessages = recentMessageContextProvider.findRecentMessages(session.getId());
        AvailabilityRequest availabilityRequest = availabilityContextProvider.parseRequest(currentMessage.getMessage());

        return ConversationContext.builder()
                .clinic(clinicContextFactory.build())
                .botCapabilities(botCapabilitiesFactory.build())
                .currentSession(session)
                .currentMessage(currentMessage)
                .lastBotMessage(recentMessageContextProvider.findLastBotMessage(recentMessages))
                .recentMessages(recentMessages)
                .allowedStates(decisionRulesProvider.allowedStates())
                .allowedIntents(decisionRulesProvider.allowedIntents())
                .requiredLeadFields(decisionRulesProvider.requiredLeadFields())
                .availabilityRequest(availabilityContextProvider.nullableRequest(availabilityRequest))
                .availabilitySuggestions(availabilityContextProvider.buildSuggestions(availabilityRequest))
                .mandatoryRules(decisionRulesProvider.mandatoryRules())
                .allowedWaitingFields(decisionRulesProvider.allowedWaitingFields())
                .build();
    }
}
