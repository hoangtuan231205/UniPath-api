package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.entity.GoogleAccountLinkSession;
import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.repository.GoogleAccountLinkSessionRepository;
import com.example.unipathapi.auth.repository.GoogleOnboardingSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleAuthDatabaseEntityPhase1Test {

    @Test
    @DisplayName("User entity cho phép password = null cho tài khoản Google-only")
    void testUserPasswordNullableForGoogleUser() {
        User googleUser = new User();
        googleUser.setEmail("alex.google@gmail.com");
        googleUser.setPassword(null);
        googleUser.setRole("CANDIDATE");
        googleUser.setIsActive(true);

        assertNull(googleUser.getPassword(), "Mật khẩu của tài khoản thuần Google phải là null");
        assertEquals("alex.google@gmail.com", googleUser.getEmail());
        assertEquals("CANDIDATE", googleUser.getRole());
        assertTrue(googleUser.getIsActive());
    }

    @Test
    @DisplayName("User entity vẫn lưu password bình thường cho tài khoản truyền thống")
    void testUserPasswordRetainedForNormalUser() {
        User normalUser = new User();
        normalUser.setEmail("standard@example.com");
        normalUser.setPassword("RawSecretPass123");
        normalUser.setRole("EMPLOYER");
        normalUser.setIsActive(true);

        assertNotNull(normalUser.getPassword());
        assertEquals("RawSecretPass123", normalUser.getPassword());
    }

    @Test
    @DisplayName("UserAuthProvider entity khởi tạo và mapping đúng các trường")
    void testUserAuthProviderEntityMapping() {
        User user = new User();
        user.setId(101);
        user.setEmail("user101@gmail.com");

        UserAuthProvider provider = new UserAuthProvider(user, "GOOGLE", "google-sub-123456789", "user101@gmail.com");
        provider.setId(1);

        assertEquals(1, provider.getId());
        assertEquals(user, provider.getUser());
        assertEquals("GOOGLE", provider.getProvider());
        assertEquals("google-sub-123456789", provider.getProviderUserId());
        assertEquals("user101@gmail.com", provider.getEmail());
    }

    @Test
    @DisplayName("GoogleOnboardingSession entity chỉ lưu token_hash và có thời hạn")
    void testGoogleOnboardingSessionEntity() {
        String tokenHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        GoogleOnboardingSession session = new GoogleOnboardingSession(
                tokenHash,
                "google-sub-999",
                "newbie@gmail.com",
                "New Student",
                "https://lh3.googleusercontent.com/avatar.jpg",
                expiresAt
        );
        session.setId(10);

        assertEquals(10, session.getId());
        assertEquals(tokenHash, session.getTokenHash());
        assertEquals("google-sub-999", session.getGoogleSub());
        assertEquals("newbie@gmail.com", session.getEmail());
        assertEquals("New Student", session.getName());
        assertEquals("https://lh3.googleusercontent.com/avatar.jpg", session.getPicture());
        assertFalse(session.getIsUsed(), "Session mới phải có is_used = false");
        assertTrue(session.getExpiresAt().isAfter(LocalDateTime.now()), "Session phải còn hạn");
    }

    @Test
    @DisplayName("GoogleAccountLinkSession entity lưu link_token_hash với userId hợp lệ")
    void testGoogleAccountLinkSessionEntity() {
        String linkTokenHash = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        GoogleAccountLinkSession linkSession = new GoogleAccountLinkSession(
                linkTokenHash,
                55,
                "google-sub-link-55",
                "existing@gmail.com",
                expiresAt
        );
        linkSession.setId(5);

        assertEquals(5, linkSession.getId());
        assertEquals(linkTokenHash, linkSession.getLinkTokenHash());
        assertEquals(55, linkSession.getUserId());
        assertEquals("google-sub-link-55", linkSession.getGoogleSub());
        assertEquals("existing@gmail.com", linkSession.getEmail());
        assertFalse(linkSession.getIsUsed());
        assertTrue(linkSession.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Kiểm tra đầy đủ phương thức cần thiết trên UserAuthProviderRepository")
    void testUserAuthProviderRepositoryContract() throws NoSuchMethodException {
        Method m1 = UserAuthProviderRepository.class.getMethod("findByProviderAndProviderUserId", String.class, String.class);
        assertNotNull(m1);

        Method m2 = UserAuthProviderRepository.class.getMethod("findByUserIdAndProvider", Integer.class, String.class);
        assertNotNull(m2);

        Method m3 = UserAuthProviderRepository.class.getMethod("existsByProviderAndProviderUserId", String.class, String.class);
        assertNotNull(m3);

        Method m4 = UserAuthProviderRepository.class.getMethod("existsByUserIdAndProvider", Integer.class, String.class);
        assertNotNull(m4);
    }

    @Test
    @DisplayName("Kiểm tra đầy đủ phương thức cần thiết trên GoogleOnboardingSessionRepository")
    void testGoogleOnboardingSessionRepositoryContract() throws NoSuchMethodException {
        Method m1 = GoogleOnboardingSessionRepository.class.getMethod("findByTokenHash", String.class);
        assertNotNull(m1);

        Method m2 = GoogleOnboardingSessionRepository.class.getMethod("consumeSession", String.class, LocalDateTime.class);
        assertNotNull(m2);
        org.springframework.data.jpa.repository.Modifying modifying = m2.getAnnotation(org.springframework.data.jpa.repository.Modifying.class);
        assertNotNull(modifying);
        assertTrue(modifying.clearAutomatically(), "consumeSession phải có clearAutomatically = true để dọn dẹp L1 cache");
        assertTrue(modifying.flushAutomatically(), "consumeSession phải có flushAutomatically = true để đẩy thay đổi trước khi update");

        Method m3 = GoogleOnboardingSessionRepository.class.getMethod("deleteExpiredSessions", LocalDateTime.class);
        assertNotNull(m3);
    }

    @Test
    @DisplayName("Kiểm tra đầy đủ phương thức cần thiết trên GoogleAccountLinkSessionRepository")
    void testGoogleAccountLinkSessionRepositoryContract() throws NoSuchMethodException {
        Method m1 = GoogleAccountLinkSessionRepository.class.getMethod("findByLinkTokenHash", String.class);
        assertNotNull(m1);

        Method m2 = GoogleAccountLinkSessionRepository.class.getMethod("consumeSession", String.class, LocalDateTime.class);
        assertNotNull(m2);
        org.springframework.data.jpa.repository.Modifying modifying = m2.getAnnotation(org.springframework.data.jpa.repository.Modifying.class);
        assertNotNull(modifying);
        assertTrue(modifying.clearAutomatically(), "consumeSession phải có clearAutomatically = true");
        assertTrue(modifying.flushAutomatically(), "consumeSession phải có flushAutomatically = true");

        Method m3 = GoogleAccountLinkSessionRepository.class.getMethod("deleteExpiredSessions", LocalDateTime.class);
        assertNotNull(m3);
    }
}
