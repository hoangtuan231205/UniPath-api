package com.example.unipathapi.auth.repository;

import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GoogleOnboardingSessionRepository extends JpaRepository<GoogleOnboardingSession, Integer> {

    Optional<GoogleOnboardingSession> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GoogleOnboardingSession s SET s.isUsed = true WHERE s.tokenHash = :tokenHash AND s.isUsed = false AND s.expiresAt > :now")
    int consumeSession(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GoogleOnboardingSession s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
}

