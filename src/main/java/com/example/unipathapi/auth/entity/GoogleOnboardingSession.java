package com.example.unipathapi.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "google_onboarding_sessions")
public class GoogleOnboardingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "google_sub", nullable = false, length = 100)
    private String googleSub;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "picture", length = 500)
    private String picture;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public GoogleOnboardingSession(String tokenHash, String googleSub, String email, String name, String picture, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.googleSub = googleSub;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }
}

