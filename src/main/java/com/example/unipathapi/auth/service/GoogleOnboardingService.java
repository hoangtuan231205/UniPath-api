package com.example.unipathapi.auth.service;

import com.example.unipathapi.auth.dto.request.GoogleCandidateOnboardingRequest;
import com.example.unipathapi.auth.dto.request.GoogleEmployerOnboardingRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.auth.entity.GoogleOnboardingSession;
import com.example.unipathapi.auth.entity.UserAuthProvider;
import com.example.unipathapi.auth.repository.GoogleOnboardingSessionRepository;
import com.example.unipathapi.auth.repository.UserAuthProviderRepository;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service implementing Google Onboarding for Candidate and Employer roles.
 * Securely consumes onboarding sessions created in Phase 3.1 and completes account registration.
 */
@Service
@RequiredArgsConstructor
public class GoogleOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOnboardingService.class);

    private final GoogleOnboardingSessionRepository onboardingSessionRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final CompanyRepository companyRepository;
    private final CompanyJoinRequestRepository joinRequestRepository;
    private final CompanyManagementService companyService;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final SecureTokenService secureTokenService;
    private final JwtUtil jwtUtil;

    /**
     * Completes candidate registration using a valid Google onboarding session.
     *
     * @param request the candidate onboarding request containing the raw token and profile fields
     * @return standard UniPath AuthResponse containing JWT, userId, role, and message
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse completeCandidateOnboarding(GoogleCandidateOnboardingRequest request) {
        if (request == null || request.getOnboardingToken() == null || request.getOnboardingToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Onboarding token không được để trống");
        }

        String rawToken = request.getOnboardingToken().trim();
        String tokenHash = secureTokenService.hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now();
        GoogleOnboardingSession session = validateAndConsumeSession(tokenHash, now);

        String googleSub = session.getGoogleSub();
        String email = session.getEmail().trim().toLowerCase();
        String googleName = session.getName();
        String googlePicture = session.getPicture();

        // Enforce uniqueness constraints
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email đã tồn tại trong hệ thống");
        }
        if (userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", googleSub)) {
            throw new IllegalStateException("Tài khoản Google này đã được liên kết với một tài khoản khác");
        }

        // Determine candidate full name (prefer user input if non-empty, otherwise fallback to Google name)
        String fullName = (request.getFullName() != null && !request.getFullName().trim().isEmpty())
                ? request.getFullName().trim()
                : (googleName != null ? googleName.trim() : "");
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }

        // 1. Create User
        User user = new User();
        user.setEmail(email);
        user.setPassword(null);
        user.setRole("CANDIDATE");
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        // 2. Create CandidateProfile
        CandidateProfile profile = new CandidateProfile();
        profile.setUser(savedUser);
        profile.setFullName(fullName);
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setUniversityName(request.getUniversityName());
        profile.setMajor(request.getMajor());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setSkills(request.getSkills());
        profile.setAvatarUrl(resolveSafeAvatarUrl(googlePicture));
        candidateProfileRepository.save(profile);

        // 3. Create UserAuthProvider
        UserAuthProvider authProvider = new UserAuthProvider(savedUser, "GOOGLE", googleSub, email);
        userAuthProviderRepository.save(authProvider);

        // Invalidate any residual onboarding sessions for this Google identity
        onboardingSessionRepository.invalidateUnusedSessionsByGoogleSub(googleSub);

        log.info("Candidate Google onboarding completed successfully for user ID: {}", savedUser.getId());

        String jwt = jwtUtil.generateToken(savedUser.getId(), savedUser.getRole());
        return new AuthResponse(jwt, String.valueOf(savedUser.getId()), savedUser.getRole(), "Đăng ký tài khoản Ứng viên bằng Google thành công!");
    }

    /**
     * Completes employer registration using a valid Google onboarding session.
     *
     * @param request the employer onboarding request containing the raw token, profile fields, and company info
     * @return standard UniPath AuthResponse containing JWT, userId, role, and message
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse completeEmployerOnboarding(GoogleEmployerOnboardingRequest request) {
        if (request == null || request.getOnboardingToken() == null || request.getOnboardingToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Onboarding token không được để trống");
        }

        // Validate company selection rules upfront
        boolean hasExistingCompany = request.getCompanyId() != null;
        boolean hasNewCompany = request.getNewCompanyName() != null && !request.getNewCompanyName().trim().isEmpty();

        if (hasExistingCompany && hasNewCompany) {
            throw new IllegalArgumentException("Không được đồng thời chọn công ty có sẵn và đề xuất công ty mới");
        }
        if (!hasExistingCompany && !hasNewCompany) {
            throw new IllegalArgumentException("Vui lòng chọn một công ty đang làm việc hoặc nhập tên công ty mới");
        }

        String rawToken = request.getOnboardingToken().trim();
        String tokenHash = secureTokenService.hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now();
        GoogleOnboardingSession session = validateAndConsumeSession(tokenHash, now);

        String googleSub = session.getGoogleSub();
        String email = session.getEmail().trim().toLowerCase();
        String googleName = session.getName();
        String googlePicture = session.getPicture();

        // Enforce uniqueness constraints
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email đã tồn tại trong hệ thống");
        }
        if (userAuthProviderRepository.existsByProviderAndProviderUserId("GOOGLE", googleSub)) {
            throw new IllegalStateException("Tài khoản Google này đã được liên kết với một tài khoản khác");
        }

        // Determine employer full name
        String fullName = (request.getFullName() != null && !request.getFullName().trim().isEmpty())
                ? request.getFullName().trim()
                : (googleName != null ? googleName.trim() : "");
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }

        // 1. Create User
        User user = new User();
        user.setEmail(email);
        user.setPassword(null);
        user.setRole("EMPLOYER");
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        // 2. Create EmployerProfile
        EmployerProfile profile = new EmployerProfile();
        profile.setUser(savedUser);
        profile.setFullName(fullName);
        profile.setPhone(request.getPhone());
        profile.setPosition(request.getPosition());
        profile.setBio(request.getBio());
        profile.setAvatarUrl(resolveSafeAvatarUrl(googlePicture));
        employerProfileRepository.save(profile);

        // 3. Handle Company Registration Flow
        if (hasExistingCompany) {
            Company comp = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Công ty được chọn không tồn tại"));

            if (!"APPROVED".equalsIgnoreCase(comp.getStatus())) {
                throw new IllegalStateException("Công ty này chưa được phê duyệt chính thức");
            }

            CompanyJoinRequest joinReq = new CompanyJoinRequest();
            joinReq.setUser(savedUser);
            joinReq.setCompany(comp);
            joinReq.setStatus("PENDING");
            joinReq.setMessage("Đăng ký tài khoản Nhà tuyển dụng và xin gia nhập công ty qua Google");
            joinRequestRepository.save(joinReq);
        } else {
            CompanyProposeRequest propReq = new CompanyProposeRequest();
            propReq.setCompanyName(request.getNewCompanyName());
            propReq.setContactEmail(request.getNewCompanyContactEmail());
            propReq.setTaxCode(request.getNewCompanyTaxCode());
            propReq.setCompanyScale(request.getNewCompanyScale() != null ? request.getNewCompanyScale() : "SME");
            propReq.setPhoneNumber(request.getNewCompanyPhoneNumber());
            propReq.setWebsite(request.getNewCompanyWebsite());
            propReq.setDescription(request.getNewCompanyDescription());
            propReq.setConfirmed(Boolean.TRUE.equals(request.getConfirmed()));

            CompanyProposeResponse propRes = companyService.proposeCompany(savedUser.getId(), propReq);
            if (Boolean.TRUE.equals(propRes.getDuplicate())) {
                throw new IllegalStateException("Công ty \"" + propRes.getCompanyName() + "\" đã tồn tại trong hệ thống. Vui lòng chọn công ty này từ danh sách.");
            }
            if (Boolean.TRUE.equals(propRes.getWarning()) && !Boolean.TRUE.equals(request.getConfirmed())) {
                throw new IllegalStateException("CẢNH BÁO: Tên công ty tương tự đã tồn tại. Vui lòng xác nhận tiếp tục.");
            }
        }

        // 4. Create UserAuthProvider
        UserAuthProvider authProvider = new UserAuthProvider(savedUser, "GOOGLE", googleSub, email);
        userAuthProviderRepository.save(authProvider);

        // Invalidate residual onboarding sessions
        onboardingSessionRepository.invalidateUnusedSessionsByGoogleSub(googleSub);

        log.info("Employer Google onboarding completed successfully for user ID: {}", savedUser.getId());

        String jwt = jwtUtil.generateToken(savedUser.getId(), savedUser.getRole());
        return new AuthResponse(jwt, String.valueOf(savedUser.getId()), savedUser.getRole(), "Đăng ký tài khoản Nhà tuyển dụng bằng Google thành công!");
    }

    /**
     * Atomically validates and consumes the onboarding session.
     * Prevents race conditions and multiple account creations using the same token.
     */
    private GoogleOnboardingSession validateAndConsumeSession(String tokenHash, LocalDateTime now) {
        GoogleOnboardingSession session = onboardingSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding session không tồn tại hoặc không hợp lệ"));

        if (Boolean.TRUE.equals(session.getIsUsed())) {
            throw new IllegalStateException("Onboarding session đã được sử dụng");
        }
        if (session.getExpiresAt().isBefore(now)) {
            throw new IllegalStateException("Onboarding session đã hết hạn");
        }

        int updated = onboardingSessionRepository.consumeSession(tokenHash, now);
        if (updated == 0) {
            throw new IllegalStateException("Onboarding session đã được sử dụng hoặc đã hết hạn");
        }

        session.setIsUsed(true);
        return session;
    }

    /**
     * Resolves avatar URL safely according to DB column length constraints.
     * Preserves URL integrity: returns null instead of truncating if length > 255.
     */
    private String resolveSafeAvatarUrl(String googlePicture) {
        if (googlePicture == null || googlePicture.trim().isEmpty()) {
            return null;
        }
        String trimmed = googlePicture.trim();
        if (trimmed.length() <= 255) {
            return trimmed;
        }
        log.warn("Google picture URL exceeds 255 characters (length: {}); falling back to default null avatar to prevent corrupt URL.", trimmed.length());
        return null;
    }
}

