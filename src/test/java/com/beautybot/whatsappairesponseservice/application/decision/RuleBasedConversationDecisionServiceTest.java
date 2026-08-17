package com.beautybot.whatsappairesponseservice.application.decision;

import com.beautybot.whatsappairesponseservice.ai.RuleBasedMessageAnalyzer;
import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationContext;
import com.beautybot.whatsappairesponseservice.conversation.decision.ConversationDecision;
import com.beautybot.whatsappairesponseservice.conversation.model.ChatMessage;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.conversation.policy.HandoffPolicy;
import com.beautybot.whatsappairesponseservice.conversation.reply.AvailabilityReplyFactory;
import com.beautybot.whatsappairesponseservice.conversation.reply.BotResponseService;
import com.beautybot.whatsappairesponseservice.conversation.reply.HandoffReplyFactory;
import com.beautybot.whatsappairesponseservice.conversation.reply.HumanSummaryFactory;
import com.beautybot.whatsappairesponseservice.conversation.reply.InformationalReplyFactory;
import com.beautybot.whatsappairesponseservice.conversation.reply.LeadCollectionReplyFactory;
import com.beautybot.whatsappairesponseservice.conversation.reply.ReplyStyleNormalizer;
import com.beautybot.whatsappairesponseservice.conversation.resolver.MissingDataResolver;
import com.beautybot.whatsappairesponseservice.conversation.state.ConversationState;
import com.beautybot.whatsappairesponseservice.conversation.state.RequiredField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedConversationDecisionServiceTest {

    @Test
    void priceInquiryAlsoCollectsNextMissingLeadField() {
        RuleBasedConversationDecisionService service = service();
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .waitingForField(RequiredField.TREATMENT)
                .build();
        ConversationContext context = context(session, "Flor. Me interesan promos en botox y AH");

        ConversationDecision decision = service.decide(context);

        assertThat(decision.getNextState()).isEqualTo(ConversationState.COLLECTING_DATA);
        assertThat(decision.getNextWaitingForField()).isEqualTo(RequiredField.FIRST_TIME);
        assertThat(decision.getRequiresHuman()).isFalse();
        assertThat(decision.getReply()).contains("asesora");
        assertThat(decision.getReply().toLowerCase()).contains("primera");
        assertThat(session.getCustomerName()).isEqualTo("Flor");
        assertThat(session.getTreatmentInterest()).isEqualTo("Botox y Relleno con acido hialuronico");
    }

    @Test
    void mediaRequestGoesToHumanWithSpecificReply() {
        RuleBasedConversationDecisionService service = service();
        ConversationSession session = ConversationSession.builder()
                .id(1L)
                .phoneNumber("5491112345678")
                .state(ConversationState.COLLECTING_DATA)
                .build();
        ConversationContext context = context(session, "Me pasarias imagenes de labios?");

        ConversationDecision decision = service.decide(context);

        assertThat(decision.getNextState()).isEqualTo(ConversationState.HUMAN_HANDOFF);
        assertThat(decision.getRequiresHuman()).isTrue();
        assertThat(decision.getShouldNotifyHuman()).isTrue();
        assertThat(decision.getReply()).matches("(?s).*(fotos|imagenes).*");
    }

    private ConversationContext context(ConversationSession session, String message) {
        return ConversationContext.builder()
                .currentSession(session)
                .currentMessage(ChatMessage.builder()
                        .phoneNumber(session.getPhoneNumber())
                        .message(message)
                        .channel("WHATSAPP")
                        .build())
                .build();
    }

    private RuleBasedConversationDecisionService service() {
        BeautyBotProperties properties = new BeautyBotProperties();
        properties.setLocation("Jose Roque Funes 1723");
        properties.setOpeningHours("lunes a viernes de 9 a 18 hs");
        properties.setAttendingDoctor("Dra. Test");

        AvailabilityReplyFactory availabilityReplyFactory = new AvailabilityReplyFactory(properties);
        BotResponseService responseService = new BotResponseService(
                new HandoffReplyFactory(),
                new LeadCollectionReplyFactory(properties),
                new InformationalReplyFactory(properties, availabilityReplyFactory),
                new HumanSummaryFactory(),
                new ReplyStyleNormalizer()
        );

        return new RuleBasedConversationDecisionService(
                new RuleBasedMessageAnalyzer(),
                new HandoffPolicy(),
                new MissingDataResolver(),
                responseService
        );
    }
}
