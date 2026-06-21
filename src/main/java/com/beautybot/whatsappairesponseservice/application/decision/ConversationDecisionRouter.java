package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.application.exception.AppException;
import com.beautybot.whatsappairesponseservice.application.exception.ResponseCode;
import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.ExtractedConversationData;
import com.beautybot.whatsappairesponseservice.observability.BeautyBotMetrics;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationDecisionRouter {

    private final AiConversationDecisionService aiConversationDecisionService;
    private final RuleBasedConversationDecisionService ruleBasedConversationDecisionService;
    private final ConversationDecisionValidator decisionValidator;
    private final BeautyBotProperties properties;
    private final BeautyBotMetrics metrics;

    public ConversationDecision decide(ConversationContext context) {
        if (context != null && context.isAvailabilityLookupFailed()) {
            return decisionValidator.validateAndFix(calendarFailureHandoffDecision(context), context);
        }

        if (isAiDecisionEnabled()) {
            try {
                ConversationDecision aiDecision = aiConversationDecisionService.decide(context);
                return decisionValidator.validateAndFix(aiDecision, context);
            } catch (Exception e) {
                if (isConfigurationError(e)) {
                    throw e;
                }
                if (!isFallbackEnabled()) {
                    throw e;
                }
                metrics.ruleBasedFallback("ai_failed");
                log.warn("AI conversation decision failed. Falling back to rule-based flow. Cause: {}", e.getMessage());
            }
        }

        if (!isAiDecisionEnabled()) {
            metrics.ruleBasedFallback("ai_disabled");
        }
        ConversationDecision fallbackDecision = ruleBasedConversationDecisionService.decide(context);
        return decisionValidator.validateAndFix(fallbackDecision, context);
    }

    private boolean isAiDecisionEnabled() {
        BeautyBotProperties.Ai.Decision decision = properties.getAi().getDecision();
        return properties.getAi().isEnabled() && decision != null && decision.isEnabled();
    }

    private boolean isFallbackEnabled() {
        BeautyBotProperties.Ai.Decision decision = properties.getAi().getDecision();
        return decision == null || decision.isFallbackEnabled();
    }

    private boolean isConfigurationError(Exception exception) {
        return exception instanceof AppException appException
                && appException.getResponseCode() == ResponseCode.AI_DECISION_CONFIGURATION_ERROR;
    }

    private ConversationDecision calendarFailureHandoffDecision(ConversationContext context) {
        return ConversationDecision.builder()
                .intents(List.of(Intent.AVAILABILITY_QUESTION))
                .nextState(ConversationState.HUMAN_HANDOFF)
                .nextWaitingForField(null)
                .extractedData(ExtractedConversationData.builder().build())
                .requiresHuman(true)
                .shouldCreateLead(false)
                .shouldNotifyHuman(true)
                .shouldBotReply(true)
                .reply("En este momento no puedo consultar la disponibilidad de agenda. Derivo tu consulta con una asesora para continuar con la coordinacion.")
                .summaryForHuman("No se pudo consultar disponibilidad de Google Calendar. Revisar agenda manualmente y responder al cliente.")
                .decisionReason("Google Calendar availability lookup failed: " + context.getAvailabilityFailureReason())
                .build();
    }

}
