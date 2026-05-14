package com.beautybot.whatsappairesponseservice.lead.service.impl;

import com.beautybot.whatsappairesponseservice.lead.entity.LeadEntity;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import com.beautybot.whatsappairesponseservice.lead.service.LeadScoringService;
import org.springframework.stereotype.Service;

@Service
public class LeadScoringServiceImpl implements LeadScoringService {

    @Override
    public int calculateScore(LeadEntity lead) {
        int score = 0;
        if (hasText(lead.getPhoneNumber())) {
            score += 10;
        }
        if (hasText(lead.getCustomerName())) {
            score += 15;
        }
        if (hasText(lead.getTreatmentInterest())) {
            score += 25;
        }
        if (lead.getFirstTime() != null) {
            score += 10;
        }
        if (hasText(lead.getPreferredTime())) {
            score += 20;
        }

        LeadStatus status = lead.getStatus();
        if (status == LeadStatus.READY_FOR_HUMAN) {
            score += 20;
        } else if (status == LeadStatus.APPOINTMENT_REQUESTED) {
            score += 30;
        } else if (status == LeadStatus.APPOINTMENT_BOOKED) {
            score += 50;
        } else if (status == LeadStatus.LOST) {
            score -= 50;
        }

        return Math.max(score, 0);
    }

    @Override
    public LeadTemperature calculateTemperature(int score) {
        if (score >= 70) {
            return LeadTemperature.HOT;
        }
        if (score >= 35) {
            return LeadTemperature.WARM;
        }
        return LeadTemperature.COLD;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
