package com.example.unipathapi.auth.integration;

import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import com.example.unipathapi.auth.repository.GoogleOnboardingSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL Integration Test: Real database concurrency and atomic session consumption for Google Onboarding.
 * Verifies that PostgreSQL row-level locks guarantee exactly one transaction succeeds when two concurrent
 * requests attempt to consume the same onboarding session token.
 */
@SpringBootTest
public class GoogleOnboardingPostgresConcurrencyTest {

    @Autowired
    private GoogleOnboardingSessionRepository sessionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String TEST_TOKEN_HASH = "postgres_test_hash_concurrency_1234567890abcdef1234567890abcdef";
    private GoogleOnboardingSession testSession;

    @BeforeEach
    public void setUp() {
        // Clean up any stale test records
        sessionRepository.findByTokenHash(TEST_TOKEN_HASH).ifPresent(s -> sessionRepository.delete(s));

        GoogleOnboardingSession session = new GoogleOnboardingSession(
                TEST_TOKEN_HASH,
                "pg_test_sub_999",
                "pg_test@unipath.local",
                "PG Test User",
                "https://lh3.googleusercontent.com/avatar.png",
                LocalDateTime.now().plusMinutes(15)
        );
        session.setIsUsed(false);
        testSession = sessionRepository.save(session);
    }

    @AfterEach
    public void tearDown() {
        if (testSession != null && testSession.getId() != null) {
            sessionRepository.deleteById(testSession.getId());
        }
    }

    @Test
    @DisplayName("PostgreSQL Concurrency Test: 2 concurrent transactions consuming the same token -> Exactly 1 succeeds (1 row), 1 fails (0 rows)")
    public void testConcurrentConsumeSession_ExactlyOneSucceeds() throws Exception {
        int threadsCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch readyLatch = new CountDownLatch(threadsCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger zeroCount = new AtomicInteger(0);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronize thread start
                    TransactionTemplate tx = new TransactionTemplate(transactionManager);
                    tx.execute(status -> {
                        int rows = sessionRepository.consumeSession(TEST_TOKEN_HASH, LocalDateTime.now());
                        if (rows == 1) {
                            successCount.incrementAndGet();
                        } else {
                            zeroCount.incrementAndGet();
                        }
                        return rows;
                    });
                } catch (Exception e) {
                    zeroCount.incrementAndGet();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire both threads simultaneously

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Both threads should finish within timeout");

        // Verify PostgreSQL Row-level locking guarantee: exactly 1 updated, 1 updated 0 rows
        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 transaction được cập nhật thành công (rows=1)");
        assertEquals(1, zeroCount.get(), "Transaction còn lại nhận 0 rows do điều kiện WHERE is_used=false không còn khớp");

        // Verify in database: is_used must be true
        GoogleOnboardingSession inDb = sessionRepository.findByTokenHash(TEST_TOKEN_HASH).orElseThrow();
        assertTrue(inDb.getIsUsed(), "Trạng thái trong PostgreSQL phải là is_used=true");
    }

    @Test
    @DisplayName("PostgreSQL Rollback Test: Transaction rollback restores is_used to false in PostgreSQL")
    public void testConsumeSession_RollbackRestoresIsUsedFalse() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        try {
            tx.execute(status -> {
                int rows = sessionRepository.consumeSession(TEST_TOKEN_HASH, LocalDateTime.now());
                assertEquals(1, rows, "consumeSession phải update 1 row trong transaction");
                // Simulate an onboarding exception (e.g. Profile error, duplicate company)
                status.setRollbackOnly();
                return rows;
            });
        } catch (Exception ignored) {
        }

        // Verify after rollback: session in DB must still have is_used = false
        GoogleOnboardingSession inDb = sessionRepository.findByTokenHash(TEST_TOKEN_HASH).orElseThrow();
        assertFalse(inDb.getIsUsed(), "Sau khi transaction rollback, is_used trong PostgreSQL phải được khôi phục là false");
    }
}

