package com.example.unipathapi.auth.integration;

import com.example.unipathapi.auth.dto.request.GoogleAccountLinkRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.auth.service.GoogleAccountLinkService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL Integration Test: Real database concurrency and atomic session consumption for Google Account Linking.
 * Verifies that PostgreSQL row-level locks guarantee exactly one transaction succeeds when two concurrent
 * requests attempt to consume the same account-link session token, and verifies transactional rollback.
 */
@SpringBootTest
public class GoogleAccountLinkPostgresConcurrencyTest {

    @Autowired
    private GoogleAccountLinkSessionRepository sessionRepository;

    @Autowired
    private UserAuthProviderRepository userAuthProviderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private GoogleAccountLinkService accountLinkService;

    @Autowired
    private SecureTokenService secureTokenService;

    private static final String RAW_TEST_TOKEN = "raw_pg_test_token_concurrency_abcdef1234567890";
    private static final String TEST_GOOGLE_SUB = "pg_link_test_sub_112233";
    private static final String TEST_EMAIL = "pg_link_test@unipath.local";
    private static final String TEST_PASSWORD = "SecureTestPass123!";

    private String testTokenHash;
    private User testUser;
    private GoogleAccountLinkSession testSession;
    private UserAuthProvider createdProvider;

    @BeforeEach
    public void setUp() {
        testTokenHash = secureTokenService.hashToken(RAW_TEST_TOKEN);

        // Clean up any stale test records
        sessionRepository.findByLinkTokenHash(testTokenHash).ifPresent(s -> sessionRepository.delete(s));
        userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", TEST_GOOGLE_SUB)
                .ifPresent(p -> userAuthProviderRepository.delete(p));
        userRepository.findByEmail(TEST_EMAIL).ifPresent(u -> userRepository.delete(u));

        // Create test user
        User user = new User();
        user.setEmail(TEST_EMAIL);
        user.setPassword(TEST_PASSWORD);
        user.setRole("CANDIDATE");
        user.setIsActive(true);
        testUser = userRepository.save(user);

        // Create test link session
        GoogleAccountLinkSession session = new GoogleAccountLinkSession(
                testTokenHash,
                testUser.getId(),
                TEST_GOOGLE_SUB,
                TEST_EMAIL,
                LocalDateTime.now().plusMinutes(15)
        );
        session.setIsUsed(false);
        testSession = sessionRepository.save(session);
    }

    @AfterEach
    public void tearDown() {
        if (createdProvider != null && createdProvider.getId() != null) {
            userAuthProviderRepository.deleteById(createdProvider.getId());
        }
        if (testSession != null && testSession.getId() != null) {
            sessionRepository.deleteById(testSession.getId());
        }
        if (testUser != null && testUser.getId() != null) {
            userAuthProviderRepository.findByUserIdAndProvider(testUser.getId(), "GOOGLE")
                    .ifPresent(p -> userAuthProviderRepository.delete(p));
            userRepository.deleteById(testUser.getId());
        }
    }

    @Test
    @DisplayName("1. PostgreSQL Concurrency: 2 concurrent transactions consuming the same link token -> Exactly 1 succeeds (1 row), 1 fails (0 rows)")
    public void testConcurrentConsumeLinkSession_ExactlyOneSucceeds() throws Exception {
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
                    startLatch.await(); // Synchronize thread execution
                    TransactionTemplate tx = new TransactionTemplate(transactionManager);
                    tx.execute(status -> {
                        int rows = sessionRepository.consumeSession(testTokenHash, LocalDateTime.now());
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
        GoogleAccountLinkSession inDb = sessionRepository.findByLinkTokenHash(testTokenHash).orElseThrow();
        assertTrue(inDb.getIsUsed(), "Trạng thái trong PostgreSQL phải là is_used=true");
    }

    @Test
    @DisplayName("2. PostgreSQL Rollback: Transaction rollback restores is_used to false in PostgreSQL")
    public void testConsumeLinkSession_RollbackRestoresIsUsedFalse() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        try {
            tx.execute(status -> {
                int rows = sessionRepository.consumeSession(testTokenHash, LocalDateTime.now());
                assertEquals(1, rows, "consumeSession phải update 1 row trong transaction");
                // Simulate an application exception during account linking
                status.setRollbackOnly();
                return rows;
            });
        } catch (Exception ignored) {
        }

        // Verify after rollback: session in DB must still have is_used = false
        GoogleAccountLinkSession inDb = sessionRepository.findByLinkTokenHash(testTokenHash).orElseThrow();
        assertFalse(inDb.getIsUsed(), "Sau khi transaction rollback, is_used trong PostgreSQL phải được khôi phục là false");
    }

    @Test
    @DisplayName("3. PostgreSQL Constraint: Duplicate (provider, provider_user_id) throws DataIntegrityViolationException")
    public void testDatabaseUniqueConstraint_EnforcedByPostgres() {
        UserAuthProvider provider1 = new UserAuthProvider(testUser, "GOOGLE", TEST_GOOGLE_SUB, TEST_EMAIL);
        createdProvider = userAuthProviderRepository.saveAndFlush(provider1);

        // Attempt to create second provider with same googleSub
        User secondUser = new User();
        secondUser.setEmail("second_user@unipath.local");
        secondUser.setPassword("Pass123!");
        secondUser.setRole("CANDIDATE");
        secondUser.setIsActive(true);
        User savedSecond = userRepository.save(secondUser);

        try {
            UserAuthProvider duplicateProvider = new UserAuthProvider(savedSecond, "GOOGLE", TEST_GOOGLE_SUB, "second_user@unipath.local");
            assertThrows(DataIntegrityViolationException.class, () -> {
                userAuthProviderRepository.saveAndFlush(duplicateProvider);
            }, "Database must throw DataIntegrityViolationException on duplicate (provider, provider_user_id)");
        } finally {
            userRepository.deleteById(savedSecond.getId());
        }
    }

    @Test
    @DisplayName("4. PostgreSQL Constraint: Duplicate (user_id, provider) throws DataIntegrityViolationException")
    public void testDatabaseUniqueConstraint_UserProvider_EnforcedByPostgres() {
        UserAuthProvider provider1 = new UserAuthProvider(testUser, "GOOGLE", TEST_GOOGLE_SUB, TEST_EMAIL);
        createdProvider = userAuthProviderRepository.saveAndFlush(provider1);

        // Attempt to create second provider for same user with different googleSub
        UserAuthProvider duplicateUserProvider = new UserAuthProvider(testUser, "GOOGLE", "another_google_sub_9999", TEST_EMAIL);
        assertThrows(DataIntegrityViolationException.class, () -> {
            userAuthProviderRepository.saveAndFlush(duplicateUserProvider);
        }, "Database must throw DataIntegrityViolationException on duplicate (user_id, provider)");
    }

    @Test
    @DisplayName("5. Transaction Rollback: Wrong password causes transaction rollback, restoring session is_used=false and saving no provider")
    public void testRollback_WhenPasswordIncorrect_RestoresSessionAndNoProvider() {
        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TEST_TOKEN, "WrongPassword999!");

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));

        // Verify in PostgreSQL: is_used must still be false
        GoogleAccountLinkSession inDb = sessionRepository.findByLinkTokenHash(testTokenHash).orElseThrow();
        assertFalse(inDb.getIsUsed(), "Session is_used must remain false after rollback");

        // Verify in PostgreSQL: no UserAuthProvider created
        assertTrue(userAuthProviderRepository.findByUserIdAndProvider(testUser.getId(), "GOOGLE").isEmpty(),
                "No UserAuthProvider should exist in database");
    }

    @Test
    @DisplayName("6. Transaction Rollback: Inactive user causes rollback, restoring session is_used=false and saving no provider")
    public void testRollback_WhenAccountInactive_RestoresSessionAndNoProvider() {
        testUser.setIsActive(false);
        userRepository.save(testUser);

        GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TEST_TOKEN, TEST_PASSWORD);

        assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));

        // Verify in PostgreSQL: is_used must still be false
        GoogleAccountLinkSession inDb = sessionRepository.findByLinkTokenHash(testTokenHash).orElseThrow();
        assertFalse(inDb.getIsUsed(), "Session is_used must remain false after rollback");

        // Verify in PostgreSQL: no UserAuthProvider created
        assertTrue(userAuthProviderRepository.findByUserIdAndProvider(testUser.getId(), "GOOGLE").isEmpty(),
                "No UserAuthProvider should exist in database");
    }

    @Test
    @DisplayName("7. Transaction Rollback: Google identity already linked to another account causes rollback")
    public void testRollback_WhenGoogleSubAlreadyLinked_RestoresSessionAndNoProvider() {
        User otherUser = new User();
        otherUser.setEmail("other_linked_user@unipath.local");
        otherUser.setPassword("Pass123!");
        otherUser.setRole("CANDIDATE");
        otherUser.setIsActive(true);
        User savedOther = userRepository.save(otherUser);

        UserAuthProvider existingLink = userAuthProviderRepository.saveAndFlush(
                new UserAuthProvider(savedOther, "GOOGLE", TEST_GOOGLE_SUB, otherUser.getEmail())
        );

        try {
            GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TEST_TOKEN, TEST_PASSWORD);

            assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));

            // Verify in PostgreSQL: is_used must still be false
            GoogleAccountLinkSession inDb = sessionRepository.findByLinkTokenHash(testTokenHash).orElseThrow();
            assertFalse(inDb.getIsUsed(), "Session is_used must remain false after rollback");

            // Verify in PostgreSQL: testUser is not linked
            assertTrue(userAuthProviderRepository.findByUserIdAndProvider(testUser.getId(), "GOOGLE").isEmpty(),
                    "testUser must not be linked in database");
        } finally {
            userAuthProviderRepository.delete(existingLink);
            userRepository.deleteById(savedOther.getId());
        }
    }

    @Test
    @DisplayName("8. Transaction Rollback: User already linked to Google causes rollback")
    public void testRollback_WhenUserAlreadyHasGoogleProvider_RestoresSessionAndNoProvider() {
        UserAuthProvider existingLink = userAuthProviderRepository.saveAndFlush(
                new UserAuthProvider(testUser, "GOOGLE", "preexisting_sub_0001", testUser.getEmail())
        );

        try {
            GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TEST_TOKEN, TEST_PASSWORD);

            assertThrows(IllegalStateException.class, () -> accountLinkService.linkAccount(request));

            // Verify in PostgreSQL: is_used must still be false
            GoogleAccountLinkSession inDb = sessionRepository.findByLinkTokenHash(testTokenHash).orElseThrow();
            assertFalse(inDb.getIsUsed(), "Session is_used must remain false after rollback");

            // Verify in PostgreSQL: no second provider for this Google identity
            assertTrue(userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", TEST_GOOGLE_SUB).isEmpty(),
                    "No provider for TEST_GOOGLE_SUB should exist");
        } finally {
            userAuthProviderRepository.delete(existingLink);
        }
    }

    @Test
    @DisplayName("9. Transaction Rollback: Residual sessions for same Google identity are NOT invalidated when transaction rolls back")
    public void testRollback_ResidualSessionsNotInvalidatedOnFailure() {
        String secondToken = "raw_residual_token_22222";
        String secondHash = secureTokenService.hashToken(secondToken);

        GoogleAccountLinkSession session2 = new GoogleAccountLinkSession(
                secondHash,
                testUser.getId(),
                TEST_GOOGLE_SUB,
                TEST_EMAIL,
                LocalDateTime.now().plusMinutes(15)
        );
        session2.setIsUsed(false);
        session2 = sessionRepository.save(session2);

        try {
            // Attempt link with first session and wrong password -> fails
            GoogleAccountLinkRequest request = new GoogleAccountLinkRequest(RAW_TEST_TOKEN, "WrongPassword!");
            assertThrows(IllegalArgumentException.class, () -> accountLinkService.linkAccount(request));

            // Verify in PostgreSQL: session 1 is rolled back to false
            GoogleAccountLinkSession inDb1 = sessionRepository.findByLinkTokenHash(testTokenHash).orElseThrow();
            assertFalse(inDb1.getIsUsed(), "Session 1 must be rolled back to is_used=false");

            // Verify in PostgreSQL: session 2 remains is_used=false (was NOT invalidated)
            GoogleAccountLinkSession inDb2 = sessionRepository.findByLinkTokenHash(secondHash).orElseThrow();
            assertFalse(inDb2.getIsUsed(), "Residual session 2 must NOT be invalidated when transaction rolls back");
        } finally {
            sessionRepository.delete(session2);
        }
    }

    @Test
    @DisplayName("10. PostgreSQL Concurrency: 2 concurrent transactions linking same googleSub to 2 different users -> Exactly 1 succeeds, 1 fails safely")
    public void testConcurrentLinking_SameGoogleSub_ExactlyOneSucceeds() throws Exception {
        String sharedGoogleSub = "shared_sub_concurrent_test_888999";

        // Create second user
        User user2 = new User();
        user2.setEmail("concurrent_user2@unipath.local");
        user2.setPassword(TEST_PASSWORD);
        user2.setRole("CANDIDATE");
        user2.setIsActive(true);
        User savedUser2 = userRepository.save(user2);

        // Raw tokens & sessions for both users sharing same Google sub
        String rawToken1 = "concurrent_raw_token_user1_1111111111";
        String rawToken2 = "concurrent_raw_token_user2_2222222222";
        String hash1 = secureTokenService.hashToken(rawToken1);
        String hash2 = secureTokenService.hashToken(rawToken2);

        GoogleAccountLinkSession session1 = new GoogleAccountLinkSession(
                hash1, testUser.getId(), sharedGoogleSub, testUser.getEmail(), LocalDateTime.now().plusMinutes(15)
        );
        session1.setIsUsed(false);
        session1 = sessionRepository.save(session1);

        GoogleAccountLinkSession session2 = new GoogleAccountLinkSession(
                hash2, savedUser2.getId(), sharedGoogleSub, savedUser2.getEmail(), LocalDateTime.now().plusMinutes(15)
        );
        session2.setIsUsed(false);
        session2 = sessionRepository.save(session2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    accountLinkService.linkAccount(new GoogleAccountLinkRequest(rawToken1, TEST_PASSWORD));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    accountLinkService.linkAccount(new GoogleAccountLinkRequest(rawToken2, TEST_PASSWORD));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown(); // Fire concurrently

            executor.shutdown();
            boolean done = executor.awaitTermination(10, TimeUnit.SECONDS);
            assertTrue(done);

            // Exactly 1 must succeed, exactly 1 must fail safely
            assertEquals(1, successCount.get(), "Chỉ duy nhất 1 request liên kết được phép thành công");
            assertEquals(1, failCount.get(), "Request đồng thời còn lại phải thất bại an toàn");

            // Verify in PostgreSQL: exactly 1 provider exists for this shared googleSub
            var providersInDb = userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", sharedGoogleSub);
            assertTrue(providersInDb.isPresent(), "Phải có đúng 1 provider được lưu trong database");

            // Verify no partial link: exactly 1 user has the provider, the losing user has no provider
            boolean user1Has = userAuthProviderRepository.findByUserIdAndProvider(testUser.getId(), "GOOGLE").isPresent();
            boolean user2Has = userAuthProviderRepository.findByUserIdAndProvider(savedUser2.getId(), "GOOGLE").isPresent();
            assertTrue(user1Has ^ user2Has,
                    "Chính xác 1 user được liên kết, user thất bại không có provider trong DB (không có liên kết một phần)");

            // Verify both sessions ended up is_used=true (1 consumed by winner, 1 invalidated by step 8 session cleanup)
            GoogleAccountLinkSession dbSession1 = sessionRepository.findByLinkTokenHash(hash1).orElseThrow();
            GoogleAccountLinkSession dbSession2 = sessionRepository.findByLinkTokenHash(hash2).orElseThrow();
            assertTrue(dbSession1.getIsUsed() && dbSession2.getIsUsed(),
                    "Cả 2 session đều có is_used=true (1 session được consume thành công, session còn lại được invalidate sạch sẽ)");

        } finally {
            userAuthProviderRepository.findByProviderAndProviderUserId("GOOGLE", sharedGoogleSub)
                    .ifPresent(p -> userAuthProviderRepository.delete(p));
            sessionRepository.delete(session1);
            sessionRepository.delete(session2);
            userRepository.deleteById(savedUser2.getId());
        }
    }

    @Test
    @DisplayName("11. PostgreSQL Concurrency: 2 concurrent transactions linking 2 different googleSubs to same user -> Exactly 1 succeeds, 1 fails safely")
    public void testConcurrentLinking_SameUserDifferentGoogleSub_ExactlyOneSucceeds() throws Exception {
        String googleSubA = "sub_concurrent_user_diff_A_1111";
        String googleSubB = "sub_concurrent_user_diff_B_2222";

        String rawTokenA = "concurrent_raw_token_subA_aaaaaaaaaa";
        String rawTokenB = "concurrent_raw_token_subB_bbbbbbbbbb";
        String hashA = secureTokenService.hashToken(rawTokenA);
        String hashB = secureTokenService.hashToken(rawTokenB);

        GoogleAccountLinkSession sessionA = new GoogleAccountLinkSession(
                hashA, testUser.getId(), googleSubA, testUser.getEmail(), LocalDateTime.now().plusMinutes(15)
        );
        sessionA.setIsUsed(false);
        sessionA = sessionRepository.save(sessionA);

        GoogleAccountLinkSession sessionB = new GoogleAccountLinkSession(
                hashB, testUser.getId(), googleSubB, testUser.getEmail(), LocalDateTime.now().plusMinutes(15)
        );
        sessionB.setIsUsed(false);
        sessionB = sessionRepository.save(sessionB);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    accountLinkService.linkAccount(new GoogleAccountLinkRequest(rawTokenA, TEST_PASSWORD));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    accountLinkService.linkAccount(new GoogleAccountLinkRequest(rawTokenB, TEST_PASSWORD));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });

            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown(); // Fire concurrently

            executor.shutdown();
            boolean done = executor.awaitTermination(10, TimeUnit.SECONDS);
            assertTrue(done);

            // Exactly 1 succeeds, 1 fails
            assertEquals(1, successCount.get(), "Chỉ duy nhất 1 request liên kết được phép thành công cho cùng user");
            assertEquals(1, failCount.get(), "Request đồng thời còn lại phải thất bại an toàn");

            // Verify in PostgreSQL: testUser has exactly 1 GOOGLE provider
            var providerInDb = userAuthProviderRepository.findByUserIdAndProvider(testUser.getId(), "GOOGLE");
            assertTrue(providerInDb.isPresent(), "testUser phải có đúng 1 provider GOOGLE trong database");

            // Verify the failed transaction's session: must be rolled back to is_used=false
            GoogleAccountLinkSession dbSessionA = sessionRepository.findByLinkTokenHash(hashA).orElseThrow();
            GoogleAccountLinkSession dbSessionB = sessionRepository.findByLinkTokenHash(hashB).orElseThrow();
            assertTrue(dbSessionA.getIsUsed() ^ dbSessionB.getIsUsed(),
                    "Đúng 1 session có is_used=true, session của transaction thất bại phải rollback về is_used=false");

        } finally {
            userAuthProviderRepository.findByUserIdAndProvider(testUser.getId(), "GOOGLE")
                    .ifPresent(p -> userAuthProviderRepository.delete(p));
            sessionRepository.delete(sessionA);
            sessionRepository.delete(sessionB);
        }
    }
}
