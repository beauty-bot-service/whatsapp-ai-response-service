package com.beautybot.whatsappairesponseservice.application.promotion;

import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.decision.DecisionSource;
import com.beautybot.whatsappairesponseservice.conversation.reply.LeadCollectionReplyFactory;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.Intent;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import com.beautybot.whatsappairesponseservice.promotion.PromotionCatalog;
import com.beautybot.whatsappairesponseservice.promotion.PromotionContent;
import com.beautybot.whatsappairesponseservice.promotion.PromotionDeliveryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ConversationPromotionPolicy {

    private static final int MAX_COMPOSED_REPLY_LENGTH = 4000;
    private static final Set<Intent> BLOCKING_INTENTS = Set.of(
            Intent.MEDICAL_QUESTION,
            Intent.HUMAN_REQUEST,
            Intent.COMPLAINT,
            Intent.RESCHEDULE,
            Intent.CANCEL
    );

    private final PromotionCatalog promotionCatalog;
    private final PromotionDeliveryRegistry promotionDeliveryRegistry;
    private final LeadCollectionReplyFactory leadCollectionReplyFactory;

    public ConversationDecision enrich(
            Long clinicId,
            ConversationContext context,
            ConversationDecision decision
    ) {
        if (decision == null || context == null || context.getCurrentMessage() == null || hasBlockingIntent(decision)) {
            return decision;
        }

        List<PromotionContent> matches = promotionDeliveryRegistry.filterUndelivered(
                context.getCurrentSession().getId(),
                resolveMatches(clinicId, context, decision)
        );
        if (matches.isEmpty()) {
            decision.setMatchedPromotionCodes(List.of());
            return decision;
        }

        ComposedPromotions composed = composePromotionBodies(matches);
        String continuation = buildContinuation(context, decision);
        String reply = appendIfFits(composed.reply(), continuation);

        promotionDeliveryRegistry.recordDelivered(
                context.getCurrentSession().getId(),
                composed.includedPromotions()
        );

        decision.setMatchedPromotionCodes(composed.includedPromotions().stream().map(PromotionContent::code).toList());
        decision.setReply(reply);
        decision.setShouldBotReply(true);
        decision.setIntents(addTreatmentIntent(decision.getIntents()));
        decision.setDecisionReason(appendReason(
                decision.getDecisionReason(),
                "Configured promotions added by backend: "
                        + String.join(", ", decision.getMatchedPromotionCodes())
                        + "."
        ));
        return decision;
    }

    private List<PromotionContent> resolveMatches(
            Long clinicId,
            ConversationContext context,
            ConversationDecision decision
    ) {
        if (decision.getMatchedPromotionCodes() != null && !decision.getMatchedPromotionCodes().isEmpty()) {
            List<PromotionContent> selected = promotionCatalog.findActiveByCodes(
                    clinicId,
                    decision.getMatchedPromotionCodes()
            );
            if (!selected.isEmpty()) {
                return selected;
            }
        }
        if (decision.getSource() == DecisionSource.RULE_BASED
                && decision.getIntents() != null
                && decision.getIntents().contains(Intent.PRICE_QUESTION)) {
            return promotionCatalog.match(clinicId, context.getCurrentMessage().getMessage());
        }
        return List.of();
    }

    private ComposedPromotions composePromotionBodies(List<PromotionContent> matches) {
        StringBuilder reply = new StringBuilder();
        List<PromotionContent> includedPromotions = new ArrayList<>();
        for (PromotionContent match : matches) {
            if (!hasText(match.messageBody())) {
                continue;
            }
            String separator = reply.isEmpty() ? "" : "\n\n";
            String candidate = separator + match.messageBody().trim();
            if (reply.length() + candidate.length() > MAX_COMPOSED_REPLY_LENGTH) {
                continue;
            }
            reply.append(candidate);
            includedPromotions.add(match);
        }
        return new ComposedPromotions(reply.toString(), List.copyOf(includedPromotions));
    }

    private String buildContinuation(ConversationContext context, ConversationDecision decision) {
        RequiredField waitingForField = decision.getNextWaitingForField();
        if (waitingForField != null && waitingForField != RequiredField.TREATMENT) {
            return leadCollectionReplyFactory.askFor(waitingForField, context.getCurrentSession());
        }
        if ((decision.getNextState() == ConversationState.READY_FOR_HUMAN
                || decision.getNextState() == ConversationState.HUMAN_HANDOFF)
                && hasText(decision.getReply())) {
            return decision.getReply();
        }
        return null;
    }

    private String appendIfFits(String promotionReply, String continuation) {
        if (!hasText(continuation)) {
            return promotionReply;
        }
        String candidate = promotionReply + "\n\n" + continuation.trim();
        return candidate.length() <= MAX_COMPOSED_REPLY_LENGTH ? candidate : promotionReply;
    }

    private List<Intent> addTreatmentIntent(List<Intent> intents) {
        LinkedHashSet<Intent> enriched = new LinkedHashSet<>();
        if (intents != null) {
            enriched.addAll(intents);
        }
        enriched.remove(Intent.UNKNOWN);
        enriched.add(Intent.TREATMENT_INFO);
        return List.copyOf(enriched);
    }

    private boolean hasBlockingIntent(ConversationDecision decision) {
        return decision.getIntents() != null
                && decision.getIntents().stream().anyMatch(BLOCKING_INTENTS::contains);
    }

    private String appendReason(String current, String addition) {
        if (!hasText(current)) {
            return addition;
        }
        return current.trim() + " " + addition;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ComposedPromotions(String reply, List<PromotionContent> includedPromotions) {
    }
}
