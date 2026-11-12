package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.dto.request.AuthRequest;
import com.example.unipathapi.auth.dto.request.ChangePasswordRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.service.AuthService;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.company.repository.CompanyJoinRequestRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.service.CompanyManagementService;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for AuthService post-audit fixes (Null Password Handling).
 * Verifies controlled rejection of Google-only accounts (null password) on standard login & change password.
 */
public class AuthServicePostAuditTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private EmployerProfileRepository employerProfileRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyJoinRequestRepository joinRequestRepository;

    @Mock
    private CompanyManagementService companyService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Test 1: Google user with password=null trying to login is rejected with controlled error")
    public void testLogin_GoogleUserWithNullPassword_RejectedControlled() {
        User googleUser = new User();
        googleUser.setId(1);
        googleUser.setEmail("googleuser@gmail.com");
        googleUser.setPassword(null); // Google OAuth user
        googleUser.setRole("CANDIDATE");
        googleUser.setIsActive(true);

        when(userRepository.findByEmail("googleuser@gmail.com")).thenReturn(Optional.of(googleUser));

        AuthRequest request = new AuthRequest();
        request.setEmail("googleuser@gmail.com");
        request.setPassword("anyPassword123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không đúng!", ex.getMessage());
    }

    @Test
    @DisplayName("Test 2: Google user with password=null passing null password in request is rejected")
    public void testLogin_GoogleUserWithNullPassword_PassingNullRequest_Rejected() {
        User googleUser = new User();
        googleUser.setId(2);
        googleUser.setEmail("googleuser2@gmail.com");
        googleUser.setPassword(null);
        googleUser.setRole("EMPLOYER");
        googleUser.setIsActive(true);

        when(userRepository.findByEmail("googleuser2@gmail.com")).thenReturn(Optional.of(googleUser));

        AuthRequest request = new AuthRequest();
        request.setEmail("googleuser2@gmail.com");
        request.setPassword(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không đúng!", ex.getMessage());
    }

    @Test
    @DisplayName("Test 3: Standard user with matching password logs in successfully")
    public void testLogin_StandardUser_ValidPassword_Success() {
        User regularUser = new User();
        regularUser.setId(3);
        regularUser.setEmail("standard@domain.com");
        regularUser.setPassword("plainPasswordSecret");
        regularUser.setRole("CANDIDATE");
        regularUser.setIsActive(true);

        when(userRepository.findByEmail("standard@domain.com")).thenReturn(Optional.of(regularUser));
        when(jwtUtil.generateToken(3, "CANDIDATE")).thenReturn("jwt.token.valid");

        AuthRequest request = new AuthRequest();
        request.setEmail("standard@domain.com");
        request.setPassword("plainPasswordSecret");

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("jwt.token.valid", response.getToken());
        assertEquals("3", response.getUserID());
        assertEquals("CANDIDATE", response.getRole());
    }

    @Test
    @DisplayName("Test 4: Standard user with incorrect password is rejected")
    public void testLogin_StandardUser_WrongPassword_Rejected() {
        User regularUser = new User();
        regularUser.setId(4);
        regularUser.setEmail("standard@domain.com");
        regularUser.setPassword("correctPassword");
        regularUser.setRole("CANDIDATE");
        regularUser.setIsActive(true);

        when(userRepository.findByEmail("standard@domain.com")).thenReturn(Optional.of(regularUser));

        AuthRequest request = new AuthRequest();
        request.setEmail("standard@domain.com");
        request.setPassword("wrongPassword");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không đúng!", ex.getMessage());
    }

    @Test
    @DisplayName("Test 5: Inactive account is rejected even with correct password")
    public void testLogin_InactiveAccount_Rejected() {
        User inactiveUser = new User();
        inactiveUser.setId(5);
        inactiveUser.setEmail("locked@domain.com");
        inactiveUser.setPassword("password123");
        inactiveUser.setRole("CANDIDATE");
        inactiveUser.setIsActive(false);

        when(userRepository.findByEmail("locked@domain.com")).thenReturn(Optional.of(inactiveUser));

        AuthRequest request = new AuthRequest();
        request.setEmail("locked@domain.com");
        request.setPassword("password123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("bị khoá"));
    }

    @Test
    @DisplayName("Test 6: Google user with password=null calling changePassword is rejected with controlled error")
    public void testChangePassword_GoogleUserNullPassword_RejectedControlled() {
        User googleUser = new User();
        googleUser.setId(6);
        googleUser.setPassword(null);

        when(userRepository.findById(6)).thenReturn(Optional.of(googleUser));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.changePassword(6, request));
        assertEquals("Mật khẩu cũ không chính xác!", ex.getMessage());
    }

    @Test
    @DisplayName("Test 7: Standard user calling changePassword with correct old password succeeds")
    public void testChangePassword_StandardUser_Success() {
        User user = new User();
        user.setId(7);
        user.setPassword("oldPass123");

        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass123");
        request.setNewPassword("newPass456");

        String result = authService.changePassword(7, request);
        assertEquals("Đổi mật khẩu thành công!", result);
        assertEquals("newPass456", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Test 8: Standard user calling changePassword with incorrect old password is rejected")
    public void testChangePassword_StandardUser_WrongOldPassword_Rejected() {
        User user = new User();
        user.setId(8);
        user.setPassword("correctOldPass");

        when(userRepository.findById(8)).thenReturn(Optional.of(user));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongOldPass");
        request.setNewPassword("newPass456");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.changePassword(8, request));
        assertEquals("Mật khẩu cũ không chính xác!", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}

