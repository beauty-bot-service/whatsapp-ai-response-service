package com.beautybot.whatsappairesponseservice.lead.service.impl;

import com.beautybot.whatsappairesponseservice.lead.entity.LeadEntity;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeadScoringServiceImplTest {

    private final LeadScoringServiceImpl service = new LeadScoringServiceImpl();

    @Test
    void emptyLeadWithPhoneIsCold() {
        LeadEntity lead = LeadEntity.builder()
                .phoneNumber("5491122334455")
                .status(LeadStatus.NEW)
                .build();

        int score = service.calculateScore(lead);
        LeadTemperature temperature = service.calculateTemperature(score);

        assertThat(score).isEqualTo(10);
        assertThat(temperature).isEqualTo(LeadTemperature.COLD);
    }

    @Test
    void leadWithTreatmentAndNameIsWarm() {
        LeadEntity lead = LeadEntity.builder()
                .phoneNumber("5491122334455")
                .customerName("Ana Perez")
                .treatmentInterest("Botox")
                .status(LeadStatus.QUALIFYING)
                .build();

        int score = service.calculateScore(lead);
        LeadTemperature temperature = service.calculateTemperature(score);

        assertThat(score).isEqualTo(50);
        assertThat(temperature).isEqualTo(LeadTemperature.WARM);
    }

    @Test
    void appointmentRequestedWithDataIsHot() {
        LeadEntity lead = LeadEntity.builder()
                .phoneNumber("5491122334455")
                .customerName("Ana Perez")
                .treatmentInterest("Botox")
                .firstTime(Boolean.TRUE)
                .preferredTime("viernes por la tarde")
                .status(LeadStatus.APPOINTMENT_REQUESTED)
                .build();

        int score = service.calculateScore(lead);
        LeadTemperature temperature = service.calculateTemperature(score);

        assertThat(score).isEqualTo(110);
        assertThat(temperature).isEqualTo(LeadTemperature.HOT);
    }

    @Test
    void lostLeadNeverReturnsNegativeScore() {
        LeadEntity lead = LeadEntity.builder()
                .status(LeadStatus.LOST)
                .build();

        int score = service.calculateScore(lead);

        assertThat(score).isZero();
    }
}
