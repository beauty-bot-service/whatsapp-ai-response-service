package com.beautybot.whatsappairesponseservice.lead.service.impl;

import com.beautybot.whatsappairesponseservice.lead.dto.LeadResponse;
import com.beautybot.whatsappairesponseservice.lead.dto.LeadUpsertRequest;
import com.beautybot.whatsappairesponseservice.lead.entity.LeadEntity;
import com.beautybot.whatsappairesponseservice.lead.entity.LeadEventEntity;
import com.beautybot.whatsappairesponseservice.lead.mapper.LeadMapper;
import com.beautybot.whatsappairesponseservice.lead.model.LeadEventType;
import com.beautybot.whatsappairesponseservice.lead.model.LeadSource;
import com.beautybot.whatsappairesponseservice.lead.model.LeadStatus;
import com.beautybot.whatsappairesponseservice.lead.model.LeadTemperature;
import com.beautybot.whatsappairesponseservice.lead.repository.LeadEventRepository;
import com.beautybot.whatsappairesponseservice.lead.repository.LeadRepository;
import com.beautybot.whatsappairesponseservice.lead.service.LeadScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceImplTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private LeadEventRepository leadEventRepository;
    @Mock
    private LeadMapper leadMapper;
    @Mock
    private LeadScoringService leadScoringService;

    private LeadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LeadServiceImpl(leadRepository, leadEventRepository, leadMapper, leadScoringService);
    }

    @Test
    void createsLeadIfNotExists() {
        LeadUpsertRequest request = LeadUpsertRequest.builder()
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .customerName("Ana")
                .source(LeadSource.WHATSAPP_INBOUND)
                .build();

        when(leadRepository.findByClinicIdAndPhoneNumber(1L, "5491123456789")).thenReturn(Optional.empty());
        when(leadScoringService.calculateScore(any())).thenReturn(40);
        when(leadScoringService.calculateTemperature(40)).thenReturn(LeadTemperature.WARM);
        when(leadRepository.save(any())).thenAnswer(invocation -> {
            LeadEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(100L);
            }
            return entity;
        });
        when(leadMapper.toResponse(any())).thenReturn(LeadResponse.builder().id(100L).build());

        service.upsertFromConversation(request);

        ArgumentCaptor<LeadEntity> leadCaptor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository, atLeastOnce()).save(leadCaptor.capture());
        LeadEntity savedLead = leadCaptor.getAllValues().getLast();
        assertThat(savedLead.getClinicId()).isEqualTo(1L);
        assertThat(savedLead.getPhoneNumber()).isEqualTo("5491123456789");
        assertThat(savedLead.getSource()).isEqualTo(LeadSource.WHATSAPP_INBOUND);

        ArgumentCaptor<LeadEventEntity> eventCaptor = ArgumentCaptor.forClass(LeadEventEntity.class);
        verify(leadEventRepository, atLeastOnce()).save(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(LeadEventEntity::getEventType)
                .contains(LeadEventType.CREATED);
    }

    @Test
    void updatesExistingWithoutOverwritingWithNull() {
        LeadEntity existing = LeadEntity.builder()
                .id(10L)
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .customerName("Nombre Existente")
                .treatmentInterest("Botox")
                .firstTime(Boolean.TRUE)
                .preferredTime("lunes 10:00")
                .status(LeadStatus.QUALIFYING)
                .temperature(LeadTemperature.WARM)
                .score(50)
                .source(LeadSource.WHATSAPP_INBOUND)
                .build();

        LeadUpsertRequest request = LeadUpsertRequest.builder()
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .customerName(null)
                .treatmentInterest(null)
                .firstTime(null)
                .preferredTime("")
                .build();

        when(leadRepository.findByClinicIdAndPhoneNumber(1L, "5491123456789")).thenReturn(Optional.of(existing));
        when(leadScoringService.calculateScore(any())).thenReturn(50);
        when(leadScoringService.calculateTemperature(50)).thenReturn(LeadTemperature.WARM);
        when(leadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadMapper.toResponse(any())).thenReturn(LeadResponse.builder().id(10L).build());

        service.upsertFromConversation(request);

        ArgumentCaptor<LeadEntity> leadCaptor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository, atLeastOnce()).save(leadCaptor.capture());
        LeadEntity updated = leadCaptor.getAllValues().getLast();
        assertThat(updated.getCustomerName()).isEqualTo("Nombre Existente");
        assertThat(updated.getTreatmentInterest()).isEqualTo("Botox");
        assertThat(updated.getFirstTime()).isTrue();
        assertThat(updated.getPreferredTime()).isEqualTo("lunes 10:00");
    }

    @Test
    void changesStatusToReadyForHuman() {
        LeadEntity existing = LeadEntity.builder()
                .id(11L)
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .status(LeadStatus.NEW)
                .temperature(LeadTemperature.COLD)
                .score(0)
                .source(LeadSource.WHATSAPP_INBOUND)
                .build();

        LeadUpsertRequest request = LeadUpsertRequest.builder()
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .readyForHuman(true)
                .build();

        when(leadRepository.findByClinicIdAndPhoneNumber(1L, "5491123456789")).thenReturn(Optional.of(existing));
        when(leadScoringService.calculateScore(any())).thenReturn(60);
        when(leadScoringService.calculateTemperature(60)).thenReturn(LeadTemperature.WARM);
        when(leadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadMapper.toResponse(any())).thenReturn(LeadResponse.builder().id(11L).status(LeadStatus.READY_FOR_HUMAN).build());

        service.upsertFromConversation(request);

        ArgumentCaptor<LeadEntity> leadCaptor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository, atLeastOnce()).save(leadCaptor.capture());
        LeadEntity updated = leadCaptor.getAllValues().getLast();
        assertThat(updated.getStatus()).isEqualTo(LeadStatus.READY_FOR_HUMAN);
        assertThat(updated.getReadyForHumanAt()).isNotNull();
    }

    @Test
    void changesStatusToAppointmentRequested() {
        LeadEntity existing = LeadEntity.builder()
                .id(12L)
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .status(LeadStatus.QUALIFYING)
                .temperature(LeadTemperature.WARM)
                .score(40)
                .source(LeadSource.WHATSAPP_INBOUND)
                .build();

        LeadUpsertRequest request = LeadUpsertRequest.builder()
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .appointmentRequested(true)
                .build();

        when(leadRepository.findByClinicIdAndPhoneNumber(1L, "5491123456789")).thenReturn(Optional.of(existing));
        when(leadScoringService.calculateScore(any())).thenReturn(80);
        when(leadScoringService.calculateTemperature(80)).thenReturn(LeadTemperature.HOT);
        when(leadRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadMapper.toResponse(any())).thenReturn(LeadResponse.builder().id(12L).status(LeadStatus.APPOINTMENT_REQUESTED).build());

        service.upsertFromConversation(request);

        ArgumentCaptor<LeadEntity> leadCaptor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository, atLeastOnce()).save(leadCaptor.capture());
        LeadEntity updated = leadCaptor.getAllValues().getLast();
        assertThat(updated.getStatus()).isEqualTo(LeadStatus.APPOINTMENT_REQUESTED);
        assertThat(updated.getAppointmentRequestedAt()).isNotNull();
    }

    @Test
    void createsCreatedAndStatusChangedEvents() {
        LeadUpsertRequest request = LeadUpsertRequest.builder()
                .clinicId(1L)
                .phoneNumber("5491123456789")
                .readyForHuman(true)
                .build();

        when(leadRepository.findByClinicIdAndPhoneNumber(1L, "5491123456789")).thenReturn(Optional.empty());
        when(leadScoringService.calculateScore(any())).thenReturn(40);
        when(leadScoringService.calculateTemperature(40)).thenReturn(LeadTemperature.WARM);
        when(leadRepository.save(any())).thenAnswer(invocation -> {
            LeadEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(101L);
            }
            return entity;
        });
        when(leadMapper.toResponse(any())).thenReturn(LeadResponse.builder().id(101L).build());

        service.upsertFromConversation(request);

        ArgumentCaptor<LeadEventEntity> eventCaptor = ArgumentCaptor.forClass(LeadEventEntity.class);
        verify(leadEventRepository, atLeastOnce()).save(eventCaptor.capture());
        List<LeadEventType> eventTypes = eventCaptor.getAllValues().stream()
                .map(LeadEventEntity::getEventType)
                .toList();
        assertThat(eventTypes).contains(LeadEventType.CREATED, LeadEventType.STATUS_CHANGED);
    }
}
