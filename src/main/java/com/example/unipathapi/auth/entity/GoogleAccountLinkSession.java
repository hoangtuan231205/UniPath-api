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
@Table(name = "google_account_link_sessions")
public class GoogleAccountLinkSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "link_token_hash", nullable = false, unique = true, length = 64)
    private String linkTokenHash;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "google_sub", nullable = false, length = 100)
    private String googleSub;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public GoogleAccountLinkSession(String linkTokenHash, Integer userId, String googleSub, String email, LocalDateTime expiresAt) {
        this.linkTokenHash = linkTokenHash;
        this.userId = userId;
        this.googleSub = googleSub;
        this.email = email;
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }
}

