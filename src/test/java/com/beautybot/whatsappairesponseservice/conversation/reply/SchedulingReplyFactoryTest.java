package com.beautybot.whatsappairesponseservice.conversation.reply;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingReplyFactoryTest {

    @Test
    void informsOfficeHoursAndDoctorWithoutOfferingCalendarSlots() {
        BeautyBotProperties properties = properties();

        String reply = new AvailabilityReplyFactory(properties).build("Tienen turno el viernes?");

        assertThat(reply).contains("lunes a viernes de 9 a 18 hs", "Dra. Ana Perez");
        assertThat(reply).contains("sin confirmarla automaticamente");
        assertThat(reply).doesNotContain("disponible", "te puedo ofrecer");
    }

    @Test
    void asksPreferredDateForLeadWithoutConsultingAnAgenda() {
        BeautyBotProperties properties = properties();
        ConversationSession session = ConversationSession.builder()
                .customerName("Gian")
                .firstTime(false)
                .build();

        String reply = new LeadCollectionReplyFactory(properties).askFor(
                com.beautybot.whatsappairesponseservice.conversation.state.RequiredField.PREFERRED_TIME,
                session
        );

        assertThat(reply).contains("lunes a viernes de 9 a 18 hs", "Dra. Ana Perez");
        assertThat(reply.toLowerCase()).contains("fecha");
        assertThat(reply.toLowerCase()).doesNotContain("turno disponible");
    }

    private BeautyBotProperties properties() {
        BeautyBotProperties properties = new BeautyBotProperties();
        properties.setOpeningHours("lunes a viernes de 9 a 18 hs");
        properties.setAttendingDoctor("Dra. Ana Perez");
        return properties;
    }
}
