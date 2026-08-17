package com.beautybot.whatsappairesponseservice.admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUserEntity user = adminUserRepository.findByEmailIgnoreCase(username == null ? "" : username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found."));
        return new AdminPrincipal(
                user.getId(),
                user.getClinicId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isEnabled()
        );
    }
}
