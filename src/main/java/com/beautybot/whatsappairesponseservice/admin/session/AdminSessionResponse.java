package com.beautybot.whatsappairesponseservice.admin.session;

import com.beautybot.whatsappairesponseservice.admin.security.AdminRole;

public record AdminSessionResponse(
        Long userId,
        Long clinicId,
        String email,
        AdminRole role
) {
}
