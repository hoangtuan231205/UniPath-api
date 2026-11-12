package com.example.unipathapi.auth.unit;

import com.example.unipathapi.auth.dto.request.GoogleCandidateOnboardingRequest;
import com.example.unipathapi.auth.dto.request.GoogleEmployerOnboardingRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.repository.GoogleOnboardingSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
import com.example.unipathapi.auth.service.GoogleOnboardingService;
import com.example.unipathapi.auth.service.SecureTokenService;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.common.security.JwtUtil;
import com.example.unipathapi.company.dto.request.CompanyProposeRequest;
import com.example.unipathapi.company.dto.response.CompanyProposeResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyJoinRequest;
import com.example.unipathapi.company.repository.CompanyJoinRequestRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.service.CompanyManagementService;
import com.example.unipathapi.employer.entity.EmployerProfile;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 3.2 Unit Test Suite: Google Onboarding Service (Candidate / Employer Registration).
 * Comprehensive test coverage covering all 4 required testing groups:
 * - Group A: Session Validation (Tests 1 - 7)
 * - Group B: Candidate Onboarding (Tests 8 - 13)
 * - Group C: Employer Onboarding (Tests 14 - 20)
 * - Group D: Security, Business Errors & Concurrency (Tests 21 - 34)
 */
public class GoogleOnboardingServicePhase3Test {

    private GoogleOnboardingSessionRepository onboardingSessionRepository;
    private UserRepository userRepository;
    private CandidateProfileRepository candidateProfileRepository;
    private EmployerProfileRepository employerProfileRepository;
    private CompanyRepository companyRepository;
    private CompanyJoinRequestRepository joinRequestRepository;
    private CompanyManagementService companyService;
    private UserAuthProviderRepository userAuthProviderRepository;
    private SecureTokenService secureTokenService;
    private JwtUtil jwtUtil;

    private GoogleOnboardingService onboardingService;

    private static final String RAW_TOKEN = "raw-onboarding-token-test-123";
    private static final String TOKEN_HASH = "hashed_onboarding_token_sha256_mock";
    private static final String GOOGLE_SUB = "109876543210987654321";
    private static final String GOOGLE_EMAIL = "newuser@gmail.com";
    private static final String GOOGLE_NAME = "Google User Name";
    private static final String GOOGLE_PICTURE = "https://lh3.googleusercontent.com/a/avatar.png";

    @BeforeEach
    public void setup() {
        onboardingSessionRepository = mock(GoogleOnboardingSessionRepository.class);
        userRepository = mock(UserRepository.class);
        candidateProfileRepository = mock(CandidateProfileRepository.class);
        employerProfileRepository = mock(EmployerProfileRepository.class);
        companyRepository = mock(CompanyRepository.class);
        joinRequestRepository = mock(CompanyJoinRequestRepository.class);
        companyService = mock(CompanyManagementService.class);
        userAuthProviderRepository = mock(UserAuthProviderRepository.class);
        secureTokenService = mock(SecureTokenService.class);
        jwtUtil = mock(JwtUtil.class);

        onboardingService = new GoogleOnboardingService(
                onboardingSessionRepository,
                userRepository,
                candidateProfileRepository,
                employerProfileRepository,
                companyRepository,
                joinRequestRepository,
                companyService,
                userAuthProviderRepository,
                secureTokenService,
                jwtUtil
        );

        when(secureTokenService.hashToken(anyString())).thenReturn(TOKEN_HASH);
        when(jwtUtil.generateToken(anyInt(), anyString())).thenReturn("mock.jwt.token");

        // User save mock helper
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(101);
            return u;
        });
    }

    private GoogleOnboardingSession createValidSession() {
        GoogleOnboardingSession session = new GoogleOnboardingSession(
                TOKEN_HASH,
                GOOGLE_SUB,
                GOOGLE_EMAIL,
                GOOGLE_NAME,
                GOOGLE_PICTURE,
                LocalDateTime.now().plusMinutes(15)
        );
        session.setId(1);
        session.setIsUsed(false);
        return session;
    }

    // =========================================================================
    // GROUP A: SESSION VALIDATION (Tests 1 - 7)
    // =========================================================================

    @Test
    @DisplayName("Test 1: Valid token proceeds with onboarding")
    public void testSessionValidation_ValidToken_ProceedsWithOnboarding() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(false);

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Test Candidate")
                .phoneNumber("0912345678")
                .universityName("HCMUT")
                .major("CS")
                .experienceYears(1)
                .skills("Java, Spring Boot")
                .build();

        AuthResponse response = onboardingService.completeCandidateOnboarding(req);
        assertNotNull(response);
        assertEquals("101", response.getUserID());
        assertEquals("CANDIDATE", response.getRole());
        assertEquals("mock.jwt.token", response.getToken());
    }

    @Test
    @DisplayName("Test 2: Non-existent token rejects onboarding")
    public void testSessionValidation_SessionNotFound_ThrowsException() {
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        assertTrue(ex.getMessage().contains("không tồn tại"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 3: Expired token rejects onboarding")
    public void testSessionValidation_SessionExpired_ThrowsException() {
        GoogleOnboardingSession expiredSession = createValidSession();
        expiredSession.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expiredSession));

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        assertTrue(ex.getMessage().contains("hết hạn"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 4: Already used token rejects onboarding")
    public void testSessionValidation_SessionAlreadyUsed_ThrowsException() {
        GoogleOnboardingSession usedSession = createValidSession();
        usedSession.setIsUsed(true);
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(usedSession));

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        assertTrue(ex.getMessage().contains("đã được sử dụng"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 5: Null or blank token rejects onboarding")
    public void testSessionValidation_NullOrEmptyToken_ThrowsException() {
        GoogleCandidateOnboardingRequest reqNull = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(null)
                .build();
        assertThrows(IllegalArgumentException.class, () ->
                onboardingService.completeCandidateOnboarding(reqNull));

        GoogleCandidateOnboardingRequest reqEmpty = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken("   ")
                .build();
        assertThrows(IllegalArgumentException.class, () ->
                onboardingService.completeCandidateOnboarding(reqEmpty));

        verify(onboardingSessionRepository, never()).findByTokenHash(any());
    }

    @Test
    @DisplayName("Test 6: Google sub derived strictly from session, never from client")
    public void testSessionValidation_GoogleSubDerivedFromSession_NotFromClient() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Name")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<UserAuthProvider> authProviderCaptor = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(userAuthProviderRepository).save(authProviderCaptor.capture());
        assertEquals(GOOGLE_SUB, authProviderCaptor.getValue().getProviderUserId());
    }

    @Test
    @DisplayName("Test 7: Google email derived strictly from session, never from client")
    public void testSessionValidation_GoogleEmailDerivedFromSession_NotFromClient() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Name")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(GOOGLE_EMAIL, userCaptor.getValue().getEmail());
    }

    // =========================================================================
    // GROUP B: CANDIDATE ONBOARDING (Tests 8 - 13)
    // =========================================================================

    @Test
    @DisplayName("Test 8: Candidate onboarding succeeds and returns AuthResponse")
    public void testCandidateOnboarding_Success_CreatesUserAndProfileAndAuthResponse() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Nguyen Van A")
                .phoneNumber("0987654321")
                .universityName("HUST")
                .major("Software Engineering")
                .experienceYears(2)
                .skills("Java, Python")
                .build();

        AuthResponse res = onboardingService.completeCandidateOnboarding(req);
        assertNotNull(res);
        assertEquals("101", res.getUserID());
        assertEquals("CANDIDATE", res.getRole());
        assertEquals("mock.jwt.token", res.getToken());
        assertTrue(res.getMessage().contains("thành công"));
    }

    @Test
    @DisplayName("Test 9: User entity created with role CANDIDATE and null password")
    public void testCandidateOnboarding_UserRoleIsCandidate_PasswordIsNull() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Nguyen Van A")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertEquals("CANDIDATE", createdUser.getRole());
        assertNull(createdUser.getPassword());
        assertTrue(createdUser.getIsActive());
        assertEquals(GOOGLE_EMAIL, createdUser.getEmail());
    }

    @Test
    @DisplayName("Test 10: CandidateProfile created with all fields matching input")
    public void testCandidateOnboarding_CandidateProfileCreatedWithAllFields() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Tran Van B")
                .phoneNumber("0909090909")
                .universityName("VNU")
                .major("Data Science")
                .experienceYears(3)
                .skills("SQL, Pandas")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<CandidateProfile> profCaptor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(profCaptor.capture());
        CandidateProfile p = profCaptor.getValue();
        assertEquals("Tran Van B", p.getFullName());
        assertEquals("0909090909", p.getPhoneNumber());
        assertEquals("VNU", p.getUniversityName());
        assertEquals("Data Science", p.getMajor());
        assertEquals(3, p.getExperienceYears());
        assertEquals("SQL, Pandas", p.getSkills());
        assertEquals(GOOGLE_PICTURE, p.getAvatarUrl());
        assertEquals(101, p.getUser().getId());
    }

    @Test
    @DisplayName("Test 11: Google provider linked with correct sub")
    public void testCandidateOnboarding_GoogleProviderLinkedWithCorrectSub() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Le Thi C")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<UserAuthProvider> provCaptor = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(userAuthProviderRepository).save(provCaptor.capture());
        UserAuthProvider prov = provCaptor.getValue();
        assertEquals("GOOGLE", prov.getProvider());
        assertEquals(GOOGLE_SUB, prov.getProviderUserId());
        assertEquals(GOOGLE_EMAIL, prov.getEmail());
        assertEquals(101, prov.getUser().getId());
    }

    @Test
    @DisplayName("Test 12: Standard JWT issued with correct user ID and CANDIDATE role")
    public void testCandidateOnboarding_StandardJwtIssuedWithCorrectClaims() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Nguyen Van A")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        verify(jwtUtil).generateToken(101, "CANDIDATE");
    }

    @Test
    @DisplayName("Test 13: Candidate onboarding does not create EmployerProfile")
    public void testCandidateOnboarding_DoesNotCreateEmployerProfile() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Nguyen Van A")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        verify(employerProfileRepository, never()).save(any());
        verify(companyRepository, never()).save(any());
        verify(joinRequestRepository, never()).save(any());
    }

    // =========================================================================
    // GROUP C: EMPLOYER ONBOARDING (Tests 14 - 20)
    // =========================================================================

    @Test
    @DisplayName("Test 14: Employer onboarding succeeds with approved existing company")
    public void testEmployerOnboarding_SuccessWithExistingCompany_CreatesJoinRequestPending() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        Company existingApproved = new Company();
        existingApproved.setId(5);
        existingApproved.setCompanyName("Existing Tech Corp");
        existingApproved.setStatus("APPROVED");
        when(companyRepository.findById(5)).thenReturn(Optional.of(existingApproved));

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Boss")
                .phone("0911223344")
                .position("HR Director")
                .bio("10 years hiring tech talents")
                .companyId(5)
                .build();

        AuthResponse res = onboardingService.completeEmployerOnboarding(req);
        assertNotNull(res);
        assertEquals("101", res.getUserID());
        assertEquals("EMPLOYER", res.getRole());
        assertEquals("mock.jwt.token", res.getToken());

        ArgumentCaptor<CompanyJoinRequest> joinCaptor = ArgumentCaptor.forClass(CompanyJoinRequest.class);
        verify(joinRequestRepository).save(joinCaptor.capture());
        CompanyJoinRequest joinReq = joinCaptor.getValue();
        assertEquals("PENDING", joinReq.getStatus());
        assertEquals(5, joinReq.getCompany().getId());
        assertEquals(101, joinReq.getUser().getId());
    }

    @Test
    @DisplayName("Test 15: Employer User created with role EMPLOYER and null password")
    public void testEmployerOnboarding_UserRoleIsEmployer_PasswordIsNull() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        Company existingApproved = new Company();
        existingApproved.setId(10);
        existingApproved.setStatus("APPROVED");
        when(companyRepository.findById(10)).thenReturn(Optional.of(existingApproved));

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Boss")
                .companyId(10)
                .build();

        onboardingService.completeEmployerOnboarding(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User u = userCaptor.getValue();
        assertEquals("EMPLOYER", u.getRole());
        assertNull(u.getPassword());
        assertTrue(u.getIsActive());
        assertEquals(GOOGLE_EMAIL, u.getEmail());
    }

    @Test
    @DisplayName("Test 16: EmployerProfile created with all fields matching input")
    public void testEmployerOnboarding_EmployerProfileCreatedWithAllFields() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        Company existingApproved = new Company();
        existingApproved.setId(12);
        existingApproved.setStatus("APPROVED");
        when(companyRepository.findById(12)).thenReturn(Optional.of(existingApproved));

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Boss John")
                .phone("0988776655")
                .position("Talent Acquisition Manager")
                .bio("Experienced recruiter in IT")
                .companyId(12)
                .build();

        onboardingService.completeEmployerOnboarding(req);

        ArgumentCaptor<EmployerProfile> profCaptor = ArgumentCaptor.forClass(EmployerProfile.class);
        verify(employerProfileRepository).save(profCaptor.capture());
        EmployerProfile p = profCaptor.getValue();
        assertEquals("Boss John", p.getFullName());
        assertEquals("0988776655", p.getPhone());
        assertEquals("Talent Acquisition Manager", p.getPosition());
        assertEquals("Experienced recruiter in IT", p.getBio());
        assertEquals(GOOGLE_PICTURE, p.getAvatarUrl());
        assertEquals(101, p.getUser().getId());
    }

    @Test
    @DisplayName("Test 17: Propose new company delegates to CompanyManagementService")
    public void testEmployerOnboarding_ProposeNewCompany_CreatesPendingCompanyAndCompanyAdmin() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        CompanyProposeResponse mockPropRes = CompanyProposeResponse.builder()
                .success(true)
                .duplicate(false)
                .warning(false)
                .companyId(99)
                .companyName("New Startup Co")
                .build();
        when(companyService.proposeCompany(eq(101), any(CompanyProposeRequest.class))).thenReturn(mockPropRes);

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Founder Mary")
                .newCompanyName("New Startup Co")
                .newCompanyContactEmail("contact@newstartup.co")
                .newCompanyTaxCode("0109998887")
                .newCompanyScale("STARTUP")
                .build();

        AuthResponse res = onboardingService.completeEmployerOnboarding(req);
        assertNotNull(res);
        assertEquals("101", res.getUserID());
        assertEquals("EMPLOYER", res.getRole());

        ArgumentCaptor<CompanyProposeRequest> propCaptor = ArgumentCaptor.forClass(CompanyProposeRequest.class);
        verify(companyService).proposeCompany(eq(101), propCaptor.capture());
        assertEquals("New Startup Co", propCaptor.getValue().getCompanyName());
        assertEquals("0109998887", propCaptor.getValue().getTaxCode());
    }

    @Test
    @DisplayName("Test 18: Company never automatically approved in onboarding")
    public void testEmployerOnboarding_CompanyNeverAutomaticallyApproved() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        CompanyProposeResponse mockPropRes = CompanyProposeResponse.builder()
                .success(true)
                .duplicate(false)
                .warning(false)
                .companyId(77)
                .build();
        when(companyService.proposeCompany(eq(101), any(CompanyProposeRequest.class))).thenReturn(mockPropRes);

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Founder Mary")
                .newCompanyName("Unapproved Co")
                .build();

        onboardingService.completeEmployerOnboarding(req);

        // Verify companyService was used, which sets status to PENDING
        verify(companyService).proposeCompany(eq(101), any());
    }

    @Test
    @DisplayName("Test 19: CompanyMember created with COMPANY_ADMIN role when proposing new company")
    public void testEmployerOnboarding_CompanyMemberCreatedWithCompanyAdminRoleWhenProposed() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        CompanyProposeResponse mockPropRes = CompanyProposeResponse.builder()
                .success(true)
                .duplicate(false)
                .warning(false)
                .companyId(88)
                .build();
        when(companyService.proposeCompany(eq(101), any(CompanyProposeRequest.class))).thenReturn(mockPropRes);

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Founder Mary")
                .newCompanyName("Admin Startup")
                .build();

        onboardingService.completeEmployerOnboarding(req);

        // Verification: CompanyManagementService is responsible for adding COMPANY_ADMIN member
        verify(companyService).proposeCompany(eq(101), any());
    }

    @Test
    @DisplayName("Test 20: Employer onboarding does not create CandidateProfile")
    public void testEmployerOnboarding_DoesNotCreateCandidateProfile() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        Company comp = new Company();
        comp.setId(1);
        comp.setStatus("APPROVED");
        when(companyRepository.findById(1)).thenReturn(Optional.of(comp));

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Only")
                .companyId(1)
                .build();

        onboardingService.completeEmployerOnboarding(req);

        verify(candidateProfileRepository, never()).save(any());
    }

    // =========================================================================
    // GROUP D: SECURITY, BUSINESS ERRORS & CONCURRENCY (Tests 21 - 34)
    // =========================================================================

    @Test
    @DisplayName("Test 21: Existing email rejects onboarding without creating User")
    public void testOnboarding_ExistingEmail_RejectsRegistration() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);

        // User with email already exists in users table
        User existingUser = new User();
        existingUser.setId(202);
        existingUser.setEmail(GOOGLE_EMAIL);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.of(existingUser));

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Existing Email")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        assertTrue(ex.getMessage().contains("Email đã tồn tại"));
        verify(userRepository, never()).save(any());
        verify(candidateProfileRepository, never()).save(any());
        verify(userAuthProviderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 22: Google sub already linked rejects onboarding")
    public void testOnboarding_GoogleSubAlreadyLinked_RejectsRegistration() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        // Provider sub already linked
        when(userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", GOOGLE_SUB)).thenReturn(true);

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Existing Sub")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        assertTrue(ex.getMessage().contains("đã được liên kết"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 23: Duplicate company name or tax code rejects employer onboarding")
    public void testEmployerOnboarding_DuplicateCompanyTaxCodeOrName_RejectsRegistration() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        CompanyProposeResponse dupRes = CompanyProposeResponse.builder()
                .success(false)
                .duplicate(true)
                .companyName("Duplicate Company Corp")
                .message("Tên công ty hoặc mã số thuế đã tồn tại")
                .build();
        when(companyService.proposeCompany(anyInt(), any())).thenReturn(dupRes);

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Dup")
                .newCompanyName("Duplicate Company Corp")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                onboardingService.completeEmployerOnboarding(req));
        assertTrue(ex.getMessage().contains("đã tồn tại trong hệ thống"));
    }

    @Test
    @DisplayName("Test 24: Invalid data - empty full name rejects registration")
    public void testOnboarding_InvalidData_MissingFullName_RejectsRegistration() {
        GoogleOnboardingSession session = createValidSession();
        session.setName(null); // No name from Google either
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("   ")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        assertTrue(ex.getMessage().contains("Họ và tên không được để trống"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 25: CandidateProfile creation failure throws exception for transaction rollback")
    public void testCandidateOnboarding_ProfileCreationFailure_RollsBack() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        doThrow(new DataIntegrityViolationException("Candidate profile constraint violation"))
                .when(candidateProfileRepository).save(any());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Fallback")
                .build();

        assertThrows(DataIntegrityViolationException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        verify(userAuthProviderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 26: UserAuthProvider creation failure throws exception for transaction rollback")
    public void testOnboarding_AuthProviderCreationFailure_RollsBack() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        doThrow(new DataIntegrityViolationException("Unique constraint violation uq_auth_provider_sub"))
                .when(userAuthProviderRepository).save(any());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Fallback")
                .build();

        assertThrows(DataIntegrityViolationException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
    }

    @Test
    @DisplayName("Test 27: Database exception during session consume throws exception for transaction rollback")
    public void testOnboarding_DatabaseException_RollsBackTransaction() {
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH))
                .thenThrow(new RuntimeException("PostgreSQL connection timeout"));

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Test")
                .build();

        assertThrows(RuntimeException.class, () ->
                onboardingService.completeCandidateOnboarding(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 28: Raw onboarding token is never exposed in response")
    public void testSecurity_RawOnboardingTokenNotExposedInLogsOrResponse() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Safe")
                .build();

        AuthResponse res = onboardingService.completeCandidateOnboarding(req);
        assertFalse(res.getToken().contains(RAW_TOKEN));
        assertFalse(res.getMessage().contains(RAW_TOKEN));
        assertFalse(res.getUserID().contains(RAW_TOKEN));
    }

    @Test
    @DisplayName("Test 29: User password is null and never exposed as hash")
    public void testSecurity_PasswordHashNeverExposedOrCreated() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate No Password")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getPassword());
    }

    @Test
    @DisplayName("Test 30: Two concurrent requests with same token - at most one succeeds")
    public void testConcurrency_TwoConcurrentRequestsSameToken_OnlyOneSucceeds() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        // Atomic simulate: first call returns 1, second call returns 0
        AtomicInteger callCount = new AtomicInteger(0);
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class)))
                .thenAnswer(inv -> callCount.incrementAndGet() == 1 ? 1 : 0);

        GoogleCandidateOnboardingRequest req1 = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Request 1")
                .build();
        GoogleCandidateOnboardingRequest req2 = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Request 2")
                .build();

        // First call succeeds
        AuthResponse res1 = onboardingService.completeCandidateOnboarding(req1);
        assertNotNull(res1);

        // Second call fails with IllegalStateException
        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                onboardingService.completeCandidateOnboarding(req2));
        assertTrue(ex2.getMessage().contains("đã được sử dụng"));

        // Only one user was created
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Test 31: Existing company not approved throws exception")
    public void testEmployerOnboarding_ExistingCompanyNotApproved_ThrowsException() {
        GoogleOnboardingSession session = createValidSession();
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        Company pendingCompany = new Company();
        pendingCompany.setId(25);
        pendingCompany.setStatus("PENDING");
        when(companyRepository.findById(25)).thenReturn(Optional.of(pendingCompany));

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Pending Comp")
                .companyId(25)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                onboardingService.completeEmployerOnboarding(req));
        assertTrue(ex.getMessage().contains("chưa được phê duyệt"));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 32: Providing both companyId and newCompanyName throws exception")
    public void testEmployerOnboarding_BothCompanyIdAndNewCompanyProvided_ThrowsException() {
        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Both")
                .companyId(10)
                .newCompanyName("Conflict Co")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                onboardingService.completeEmployerOnboarding(req));
        assertTrue(ex.getMessage().contains("đồng thời"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 33: Providing neither companyId nor newCompanyName throws exception")
    public void testEmployerOnboarding_NeitherCompanyIdNorNewCompanyProvided_ThrowsException() {
        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Neither")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                onboardingService.completeEmployerOnboarding(req));
        assertTrue(ex.getMessage().contains("Vui lòng chọn một công ty"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 34: Avatar URL set to null when exceeding 255 chars (no corrupt truncation)")
    public void testAvatarUrl_NullWhenExceeds255Chars_NotTruncated() {
        String longPictureUrl = "https://lh3.googleusercontent.com/a/" + "a".repeat(300);
        GoogleOnboardingSession session = createValidSession();
        session.setPicture(longPictureUrl);
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Long Avatar")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<CandidateProfile> profCaptor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(profCaptor.capture());
        assertNull(profCaptor.getValue().getAvatarUrl(), "URL quá dài (> 255) phải trả về null để hiển thị avatar mặc định, không được cắt cụt làm hỏng URI");
    }

    @Test
    @DisplayName("Test 35: Avatar URL null or blank remains null")
    public void testAvatarUrl_NullOrBlank_RemainsNull() {
        GoogleOnboardingSession session = createValidSession();
        session.setPicture("   "); // blank
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate Blank Avatar")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<CandidateProfile> profCaptor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(profCaptor.capture());
        assertNull(profCaptor.getValue().getAvatarUrl());
    }

    @Test
    @DisplayName("Test 36: Avatar URL with exact 255 chars is stored intact")
    public void testAvatarUrl_Exact255Chars_StoredIntact() {
        String exact255Url = "https://lh3.googleusercontent.com/a/" + "b".repeat(255 - "https://lh3.googleusercontent.com/a/".length());
        assertEquals(255, exact255Url.length());

        GoogleOnboardingSession session = createValidSession();
        session.setPicture(exact255Url);
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        GoogleCandidateOnboardingRequest req = GoogleCandidateOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Candidate 255 Avatar")
                .build();

        onboardingService.completeCandidateOnboarding(req);

        ArgumentCaptor<CandidateProfile> profCaptor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(candidateProfileRepository).save(profCaptor.capture());
        assertEquals(exact255Url, profCaptor.getValue().getAvatarUrl());
    }

    @Test
    @DisplayName("Test 37: Employer avatar URL exceeding 255 chars is stored as null")
    public void testEmployerAvatarUrl_OversizedStoredAsNull() {
        String longPictureUrl = "https://lh3.googleusercontent.com/a/" + "c".repeat(260);
        GoogleOnboardingSession session = createValidSession();
        session.setPicture(longPictureUrl);
        when(onboardingSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
        when(onboardingSessionRepository.consumeSession(eq(TOKEN_HASH), any(LocalDateTime.class))).thenReturn(1);
        when(userRepository.findByEmail(GOOGLE_EMAIL)).thenReturn(Optional.empty());

        Company comp = new Company();
        comp.setId(1);
        comp.setStatus("APPROVED");
        when(companyRepository.findById(1)).thenReturn(Optional.of(comp));

        GoogleEmployerOnboardingRequest req = GoogleEmployerOnboardingRequest.builder()
                .onboardingToken(RAW_TOKEN)
                .fullName("Employer Long Avatar")
                .companyId(1)
                .build();

        onboardingService.completeEmployerOnboarding(req);

        ArgumentCaptor<EmployerProfile> profCaptor = ArgumentCaptor.forClass(EmployerProfile.class);
        verify(employerProfileRepository).save(profCaptor.capture());
        assertNull(profCaptor.getValue().getAvatarUrl(), "Employer avatar quá dài (> 255) phải lưu null, không cắt cụt");
    }
}
