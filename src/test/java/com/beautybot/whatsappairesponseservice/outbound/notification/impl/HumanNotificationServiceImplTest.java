package com.beautybot.whatsappairesponseservice.outbound.notification.impl;

import com.beautybot.whatsappairesponseservice.config.BeautyBotProperties;
import com.beautybot.whatsappairesponseservice.conversation.model.ConversationSession;
import com.beautybot.whatsappairesponseservice.observability.PhoneNumberMasker;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppCloudApiClient;
import com.beautybot.whatsappairesponseservice.whatsapp.WhatsAppSendResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanNotificationServiceImplTest {

    @Mock
    private WhatsAppCloudApiClient whatsAppCloudApiClient;
    @Mock
    private PhoneNumberMasker phoneNumberMasker;

    @Test
    void sendsLeadNotificationToConfiguredAdvisorPhone() {
        BeautyBotProperties properties = properties("5491199999999");
        HumanNotificationServiceImpl service = new HumanNotificationServiceImpl(
                properties,
                whatsAppCloudApiClient,
                phoneNumberMasker
        );
        ConversationSession session = session();
        when(whatsAppCloudApiClient.sendTextMessage(eq("5491199999999"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(WhatsAppSendResult.SENT);
        when(phoneNumberMasker.mask(session.getPhoneNumber())).thenReturn("****5678");

        boolean sent = service.notifyAdvisor(session);

        assertThat(sent).isTrue();
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(whatsAppCloudApiClient).sendTextMessage(eq("5491199999999"), message.capture());
        assertThat(message.getValue()).contains("Nuevo lead para asesor");
        assertThat(message.getValue()).contains("Telefono: 5491112345678");
        assertThat(message.getValue()).contains("Tratamiento: botox");
    }

    @Test
    void returnsFalseWhenAdvisorPhoneIsMissing() {
        BeautyBotProperties properties = properties(" ");
        HumanNotificationServiceImpl service = new HumanNotificationServiceImpl(
                properties,
                whatsAppCloudApiClient,
                phoneNumberMasker
        );

        boolean sent = service.notifyAdvisor(session());

        assertThat(sent).isFalse();
        verifyNoInteractions(whatsAppCloudApiClient);
    }

    @Test
    void returnsFalseWhenWhatsAppSendFails() {
        BeautyBotProperties properties = properties("5491199999999");
        HumanNotificationServiceImpl service = new HumanNotificationServiceImpl(
                properties,
                whatsAppCloudApiClient,
                phoneNumberMasker
        );
        ConversationSession session = session();
        when(whatsAppCloudApiClient.sendTextMessage(eq("5491199999999"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(WhatsAppSendResult.FAILED);
        when(phoneNumberMasker.mask(session.getPhoneNumber())).thenReturn("****5678");

        boolean sent = service.notifyAdvisor(session);

        assertThat(sent).isFalse();
    }

    private BeautyBotProperties properties(String advisorPhone) {
        BeautyBotProperties properties = new BeautyBotProperties();
        properties.setAdvisorNotificationEnabled(true);
        properties.setAdvisorNotificationPhoneNumber(advisorPhone);
        return properties;
    }

    private ConversationSession session() {
        return ConversationSession.builder()
                .id(10L)
                .phoneNumber("5491112345678")
                .customerName("Florencia")
                .treatmentInterest("botox")
                .firstTime(true)
                .preferredTime("viernes a la tarde")
                .summaryForHuman("Quiere coordinar turno.")
                .build();
    }
}
