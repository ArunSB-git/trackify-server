package com.app.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class SecurityUtil {

    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Unauthenticated");
        }

        // JWT flow
        if (auth.getPrincipal() instanceof String email) {
            return email;
        }
        OAuth2User oauthUser = (OAuth2User) auth.getPrincipal();
        return oauthUser.getAttribute("email");
    }
}
