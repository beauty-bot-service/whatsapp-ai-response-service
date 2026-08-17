package com.beautybot.whatsappairesponseservice.admin.session;

import com.beautybot.whatsappairesponseservice.admin.security.AdminPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminSessionController {

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName());
    }

    @GetMapping("/session")
    public AdminSessionResponse session(@AuthenticationPrincipal AdminPrincipal principal) {
        return new AdminSessionResponse(
                principal.userId(),
                principal.clinicId(),
                principal.email(),
                principal.role()
        );
    }
}
