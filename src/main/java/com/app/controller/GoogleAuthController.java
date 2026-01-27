package com.app.controller;

import com.app.dto.AuthResponse;
import com.app.dto.GoogleAuthRequest;
import com.app.service.GoogleAuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    public GoogleAuthController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/google")
    public AuthResponse googleLogin(@RequestBody GoogleAuthRequest request) throws Exception {
        return googleAuthService.authenticate(request.getIdToken());
    }
}
