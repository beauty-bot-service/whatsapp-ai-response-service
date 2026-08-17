package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.application.decision.context.BotCapabilitiesFactory;
import com.beautybot.whatsappairesponseservice.application.decision.context.ClinicContextFactory;
import com.beautybot.whatsappairesponseservice.application.decision.context.RecentMessageContextProvider;
import com.beautybot.whatsappairesponseservice.application.support.ClinicIdProvider;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.RecentConversationMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.promotion.PromotionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationContextBuilder {

    private final ClinicContextFactory clinicContextFactory;
    private final BotCapabilitiesFactory botCapabilitiesFactory;
    private final RecentMessageContextProvider recentMessageContextProvider;
    private final PromotionCatalog promotionCatalog;
    private final ClinicIdProvider clinicIdProvider;

    public ConversationContext build(ConversationSession session, ChatMessage currentMessage) {
        List<RecentConversationMessage> recentMessages = recentMessageContextProvider.findRecentMessages(session.getId());

        return ConversationContext.builder()
                .clinic(clinicContextFactory.build())
                .botCapabilities(botCapabilitiesFactory.build())
                .currentSession(session)
                .currentMessage(currentMessage)
                .lastBotMessage(recentMessageContextProvider.findLastBotMessage(recentMessages))
                .recentMessages(recentMessages)
                .activePromotions(promotionCatalog.findActive(clinicIdProvider.currentClinicId()))
                .build();
    }
}
