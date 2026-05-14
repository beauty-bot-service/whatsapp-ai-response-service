package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.DecisionSource;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.model.MessageAnalysis;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotResponseService {

    private final HandoffReplyFactory handoffReplyFactory;
    private final LeadCollectionReplyFactory leadCollectionReplyFactory;
    private final InformationalReplyFactory informationalReplyFactory;
    private final HumanSummaryFactory humanSummaryFactory;
    private final ReplyStyleNormalizer replyStyleNormalizer;

    public ConversationDecision askFor(RequiredField field, ConversationSession session, MessageAnalysis analysis) {
        return collecting(leadCollectionReplyFactory.askFor(field, session), analysis, field);
    }

    public ConversationDecision answerInformational(MessageAnalysis analysis) {
        return collecting(informationalReplyFactory.build(analysis), analysis, null);
    }

    public ConversationDecision handoffToHuman(ConversationSession session, MessageAnalysis analysis) {
        return ConversationDecision.builder()
                .source(DecisionSource.RULE_BASED)
                .intents(analysis.intentsOrPrimary())
                .nextState(ConversationState.HUMAN_HANDOFF)
                .nextWaitingForField(null)
                .requiresHuman(true)
                .shouldCreateLead(false)
                .shouldNotifyHuman(true)
                .shouldBotReply(true)
                .reply(replyStyleNormalizer.normalize(handoffReplyFactory.build(analysis)))
                .summaryForHuman(humanSummaryFactory.build(session))
                .decisionReason("Derivacion generada por fallback rule-based.")
                .build();
    }

    public ConversationDecision readyForHuman(ConversationSession session, MessageAnalysis analysis) {
        return ConversationDecision.builder()
                .source(DecisionSource.RULE_BASED)
                .intents(analysis.intentsOrPrimary())
                .nextState(ConversationState.READY_FOR_HUMAN)
                .nextWaitingForField(null)
                .requiresHuman(true)
                .shouldCreateLead(true)
                .shouldNotifyHuman(true)
                .shouldBotReply(true)
                .reply(replyStyleNormalizer.normalize(leadCollectionReplyFactory.readyForHuman(session)))
                .summaryForHuman(humanSummaryFactory.build(session))
                .decisionReason("Lead completo por fallback rule-based.")
                .build();
    }

    public ConversationDecision mergeReplies(ConversationDecision baseDecision, String prefixReply) {
        if (baseDecision == null || !hasText(prefixReply)) {
            return baseDecision;
        }
        String mergedReply = joinReplies(prefixReply, baseDecision.getReply());
        baseDecision.setReply(replyStyleNormalizer.normalize(mergedReply));
        baseDecision.setShouldBotReply(hasText(baseDecision.getReply()));
        return baseDecision;
    }

    private ConversationDecision collecting(String reply, MessageAnalysis analysis, RequiredField waitingForField) {
        return ConversationDecision.builder()
                .source(DecisionSource.RULE_BASED)
                .intents(analysis.intentsOrPrimary())
                .nextState(ConversationState.COLLECTING_DATA)
                .nextWaitingForField(waitingForField)
                .requiresHuman(false)
                .shouldCreateLead(false)
                .shouldNotifyHuman(false)
                .shouldBotReply(true)
                .reply(replyStyleNormalizer.normalize(reply))
                .decisionReason("Respuesta de recoleccion generada por fallback rule-based.")
                .build();
    }

    private String joinReplies(String first, String second) {
        if (!hasText(first)) {
            return second;
        }
        if (!hasText(second)) {
            return first;
        }
        return first.trim() + " " + second.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
