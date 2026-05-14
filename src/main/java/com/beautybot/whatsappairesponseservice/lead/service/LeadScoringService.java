package com.beautybot.whatsappairesponseservice.lead.service;

import com.beautybot.whatsappairesponseservice.lead.entity.LeadEntity;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;

public interface LeadScoringService {
    int calculateScore(LeadEntity lead);
    LeadTemperature calculateTemperature(int score);
}
