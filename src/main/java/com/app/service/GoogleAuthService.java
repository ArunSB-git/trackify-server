package com.app.service;

import com.app.config.JwtUtil;
import com.app.dto.AuthResponse;
import com.app.model.User;
import com.app.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public GoogleAuthService(
            GoogleIdTokenVerifier verifier,
            UserRepository userRepository,
            JwtUtil jwtUtil
    ) {
        this.verifier = verifier;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse authenticate(String idTokenString) throws Exception {

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new RuntimeException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, name, picture));

        String accessToken = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        return new AuthResponse(accessToken, refreshToken);
    }

    private User createUser(String email, String name, String picture) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setProfilePicture(picture);
        return userRepository.save(user);
    }
}
