package com.beautybot.whatsappairesponseservice.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_security_integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "beauty-bot.admin.bootstrap-email=owner@example.com",
        "beauty-bot.admin.bootstrap-password=integration-secret-123",
        "beauty-bot.ai.enabled=false",
        "beauty-bot.calendar.enabled=false",
        "beauty-bot.whatsapp.enabled=false",
        "beauty-bot.security.internal-api-key-enabled=false"
})
@AutoConfigureMockMvc
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticatedAdminCanCreateAndListPromotionsUsingCsrfProtection() throws Exception {
        mockMvc.perform(get("/api/admin/promotions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));

        CsrfCredentials anonymousCsrf = requestCsrf(null);
        MvcResult loginResult = mockMvc.perform(post("/api/admin/login")
                        .cookie(anonymousCsrf.cookie())
                        .header(anonymousCsrf.headerName(), anonymousCsrf.token())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "owner@example.com")
                        .param("password", "integration-secret-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        CsrfCredentials authenticatedCsrf = requestCsrf(session);
        mockMvc.perform(post("/api/admin/promotions")
                        .session(session)
                        .cookie(authenticatedCsrf.cookie())
                        .header(authenticatedCsrf.headerName(), authenticatedCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "botox-test",
                                  "title": "Botox test",
                                  "messageBody": "Promocion de prueba",
                                  "aliases": ["botox promo"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("botox-test"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/admin/promotions")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code == 'botox-test')]").exists());
    }

    private CsrfCredentials requestCsrf(MockHttpSession session) throws Exception {
        var request = get("/api/admin/csrf");
        if (session != null) {
            request.session(session);
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");

        assertThat(cookie).isNotNull();
        return new CsrfCredentials(response.get("token").asText(), response.get("headerName").asText(), cookie);
    }

    private record CsrfCredentials(String token, String headerName, Cookie cookie) {
    }
}
