package com.beautybot.whatsappairesponseservice.lead.controller;

import com.beautybot.whatsappairesponseservice.lead.dto.LeadResponse;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import com.beautybot.whatsappairesponseservice.lead.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/leads")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public Page<LeadResponse> search(
            @RequestHeader("clinic-id") Long clinicId,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadTemperature temperature,
            @RequestParam(required = false) String treatmentInterest,
            Pageable pageable
    ) {
        return leadService.search(clinicId, status, temperature, treatmentInterest, pageable);
    }

    @GetMapping("/{leadId}")
    public LeadResponse getById(
            @RequestHeader("clinic-id") Long clinicId,
            @PathVariable Long leadId
    ) {
        return leadService.getById(clinicId, leadId);
    }

    @PatchMapping("/{leadId}/status")
    public LeadResponse changeStatus(
            @RequestHeader("clinic-id") Long clinicId,
            @PathVariable Long leadId,
            @RequestParam("status") LeadStatus status,
            @RequestHeader(value = "changed-by", required = false) String changedBy
    ) {
        return leadService.changeStatus(clinicId, leadId, status, changedBy);
    }

    @PatchMapping("/{leadId}/assign")
    public LeadResponse assign(
            @RequestHeader("clinic-id") Long clinicId,
            @PathVariable Long leadId,
            @RequestParam("assignedToUserId") Long assignedToUserId,
            @RequestHeader(value = "assigned-by", required = false) String assignedBy
    ) {
        return leadService.assign(clinicId, leadId, assignedToUserId, assignedBy);
    }

    @PostMapping("/{leadId}/notes")
    public LeadResponse addNote(
            @RequestHeader("clinic-id") Long clinicId,
            @PathVariable Long leadId,
            @RequestBody String note,
            @RequestHeader(value = "created-by", required = false) String createdBy
    ) {
        return leadService.addNote(clinicId, leadId, note, createdBy);
    }
}
