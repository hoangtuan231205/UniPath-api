package com.example.unipathapi.auth.repository;

import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GoogleAccountLinkSessionRepository extends JpaRepository<GoogleAccountLinkSession, Integer> {

    Optional<GoogleAccountLinkSession> findByLinkTokenHash(String linkTokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GoogleAccountLinkSession s SET s.isUsed = true WHERE s.linkTokenHash = :linkTokenHash AND s.isUsed = false AND s.expiresAt > :now")
    int consumeSession(@Param("linkTokenHash") String linkTokenHash, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GoogleAccountLinkSession s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
}

