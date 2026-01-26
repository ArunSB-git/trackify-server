package com.app.controller;

import com.app.config.JwtUtil;
import com.app.model.User;
import com.app.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@RestController
public class MobileOAuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${GOOGLE_CLIENT_ID}")
    private String clientId;

    public MobileOAuthController(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/oauth2/mobile/google")
    public void googleLogin(@RequestParam("idToken") String idTokenString,
                            HttpServletResponse response) throws IOException, GeneralSecurityException {

        // Verify Google ID token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Google ID token");
            return;
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        // Save user if not exists
        userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setProfilePicture(picture);
            return userRepository.save(user);
        });

        // Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        // Redirect to Android deep link
        response.sendRedirect(
                "trackify://auth-callback?access=" + accessToken + "&refresh=" + refreshToken
        );
    }
}
