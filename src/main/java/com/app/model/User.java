package com.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // getters & setters
}
