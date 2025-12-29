package com.app.controller;

import com.app.dto.UserProfileResponse;
import com.app.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "http://localhost:5174")
@RestController
public class AuthController {

    @GetMapping("/")
    public String greet() {
        return "Hello World";
    }


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @DeleteMapping("/api/users/{userId}")
    public String deleteUser(@PathVariable UUID userId) {
        authService.deleteUser(userId);
        return "User and related data deleted successfully!";
    }

    // ✅ Logged-in user profile API
    @GetMapping("/api/users/me")
    public UserProfileResponse getMyProfile() {
        return authService.getCurrentUserProfile();
    }

    @GetMapping("/api/session-check")
    public SessionResponse checkSession(HttpSession session) {
        boolean valid = session != null && session.getAttribute("SPRING_SECURITY_CONTEXT") != null;
        return new SessionResponse(valid);
    }

    static class SessionResponse {
        private boolean valid;

        public SessionResponse(boolean valid) { this.valid = valid; }

        public boolean isValid() { return valid; }

        public void setValid(boolean valid) { this.valid = valid; }
    }
    @PostMapping("/api/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate(); // destroys JSESSIONID
        }
        return "Logged out successfully";
    }
}
