package com.beautybot.whatsappairesponseservice.conversation.decision;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDecision {
    private DecisionSource source;

    @Builder.Default
    private List<Intent> intents = new ArrayList<>();

    private ConversationState nextState;
    private RequiredField nextWaitingForField;
    private ExtractedConversationData extractedData;

    @Builder.Default
    private List<String> missingFields = new ArrayList<>();

    private Boolean requiresHuman;
    private Boolean shouldCreateLead;
    private Boolean shouldNotifyHuman;
    private Boolean shouldBotReply;
    private String reply;
    private String summaryForHuman;
    private String decisionReason;

    public Intent primaryIntent() {
        if (intents != null && !intents.isEmpty()) {
            return intents.get(0);
        }
        return Intent.UNKNOWN;
    }
}
