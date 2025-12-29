package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private String name;
    private String email;
    private String profilePicture;
    private Instant createdAt;
}
