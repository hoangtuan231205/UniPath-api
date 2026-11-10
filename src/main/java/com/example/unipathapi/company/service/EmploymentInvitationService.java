package com.example.unipathapi.company.service;

import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.company.dto.request.EmploymentInvitationCreateRequest;
import com.example.unipathapi.company.dto.request.RecruitmentInvitationCreateRequest;
import com.example.unipathapi.company.dto.response.CandidateEmploymentResponse;
import com.example.unipathapi.company.dto.response.EmployeeResponse;
import com.example.unipathapi.company.dto.response.EmploymentInvitationResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.Employment;
import com.example.unipathapi.company.entity.EmploymentInvitation;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.repository.EmploymentInvitationRepository;
import com.example.unipathapi.company.repository.EmploymentRepository;
import com.example.unipathapi.employer.entity.EmployerProfile;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.notification.entity.Notification;
import com.example.unipathapi.notification.repository.NotificationRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class EmploymentInvitationService {

    private static final Set<String> VALID_SALARY_TYPES = Set.of("HOURLY", "MONTHLY", "NEGOTIABLE", "UNPAID");

    @Autowired
    private EmploymentInvitationRepository invitationRepository;

    @Autowired
    private EmploymentRepository employmentRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired(required = false)
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // Anti-Spam / Anti-DDOS Rate Limiter & Concurrency in-flight lock
    private final java.util.concurrent.ConcurrentHashMap<Integer, Long> inFlightApplications = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> inFlightDirectInvitations = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedQueue<Long>> invitationRateLimits = new java.util.concurrent.ConcurrentHashMap<>();

    @Transactional
    public EmploymentInvitationResponse createDirectInvitation(Integer employerUserId, Integer companyId, EmploymentInvitationCreateRequest request) {
        // 0. Chống DDOS / Spam thao tác và chống gửi đồng thời
        checkInvitationRateLimit(employerUserId, companyId);
        String lockKey = "DIRECT:" + companyId + ":" + request.getCandidateId();
        Long prev = inFlightDirectInvitations.putIfAbsent(lockKey, System.currentTimeMillis());
        if (prev != null && (System.currentTimeMillis() - prev < 5000L)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lời mời cho ứng viên này đang được xử lý, vui lòng không gửi liên tục.");
        }

        try {
            return doCreateDirectInvitation(employerUserId, companyId, request);
        } finally {
            inFlightDirectInvitations.remove(lockKey);
        }
    }

    private EmploymentInvitationResponse doCreateDirectInvitation(Integer employerUserId, Integer companyId, EmploymentInvitationCreateRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Công ty không tồn tại"));

        // Phân quyền chặt chẽ: Chỉ COMPANY_ADMIN mới có quyền gửi lời mời trực tiếp ngoài tuyển dụng
        boolean isAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(companyId, employerUserId, List.of("COMPANY_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên công ty (COMPANY_ADMIN) mới có quyền gửi lời mời nhân sự trực tiếp");
        }

        // Kiểm tra dịch vụ HRM
        if (!Boolean.TRUE.equals(company.getHrmEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doanh nghiệp chưa kích hoạt dịch vụ Quản lý nhân sự (HRM)");
        }

        User employerUser = userRepository.findById(employerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng người tuyển dụng không tồn tại"));

        User candidate = userRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ứng viên không tồn tại"));

        // Chống mời khi Candidate đã có Employment ACTIVE hoặc PENDING_START tại công ty (kiểm tra cả dữ liệu cũ)
        boolean hasActiveEmployment = employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(
                companyId, candidate.getId(), List.of("ACTIVE", "PENDING_START")
        );
        if (hasActiveEmployment) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ứng viên đã có hợp đồng nhân sự đang hoạt động tại công ty");
        }

        // Chống gửi nhiều lời mời PENDING trùng lặp
        boolean hasPendingInvitation = invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(companyId, candidate.getId(), "PENDING");
        if (hasPendingInvitation) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đang có một lời mời chưa được phản hồi dành cho ứng viên này");
        }

        // Validate lương theo salaryType
        String salaryType = request.getSalaryType() != null ? request.getSalaryType().toUpperCase().trim() : "HOURLY";
        validateSalaryTypeAndValues(salaryType, request.getBaseSalaryPerHour(), request.getBaseSalaryMonthly());

        if (request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu dự kiến không được để trống");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu dự kiến không thể trong quá khứ");
        }

        EmploymentInvitation invitation = new EmploymentInvitation();
        invitation.setCompany(company);
        invitation.setCandidate(candidate);
        invitation.setApplication(null);
        invitation.setInvitationType("DIRECT");
        invitation.setStatus("PENDING");
        invitation.setPosition(request.getPosition().trim());
        invitation.setSalaryType(salaryType);
        invitation.setBaseSalaryPerHour(request.getBaseSalaryPerHour());
        invitation.setBaseSalaryMonthly(request.getBaseSalaryMonthly());
        invitation.setSalaryNote(request.getSalaryNote());
        invitation.setStartDate(request.getStartDate());
        invitation.setNote(request.getNote());
        invitation.setInvitedBy(employerUser);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        EmploymentInvitation saved = invitationRepository.save(invitation);

        // Gửi thông báo đến Candidate
        Notification notification = new Notification();
        notification.setUser(candidate);
        notification.setTitle("Lời mời nhận việc trực tiếp");
        notification.setMessage("Công ty " + company.getCompanyName() + " đã gửi lời mời bạn làm việc ở vị trí '" +
                saved.getPosition() + "'. Vui lòng kiểm tra và phản hồi.");
        notificationRepository.save(notification);

        return buildInvitationResponse(saved);
    }

    @Transactional
    public EmploymentInvitation createRecruitmentInvitation(
            Application application,
            Integer employerUserId,
            String position,
            String salaryType,
            BigDecimal baseSalaryPerHour,
            BigDecimal baseSalaryMonthly,
            String salaryNote,
            LocalDate startDate,
            String note
    ) {
        if (invitationRepository.existsByApplicationId(application.getId())) {
            return invitationRepository.findByApplicationId(application.getId()).orElse(null);
        }

        Company company = application.getJob().getCompany();
        User candidate = application.getCandidate();
        User employerUser = userRepository.findById(employerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        String finalPosition = (position != null && !position.isBlank()) ? position.trim() : application.getJob().getTitle();
        String finalSalaryType = (salaryType != null && !salaryType.isBlank()) ? salaryType.toUpperCase().trim() : "HOURLY";
        LocalDate finalStartDate = startDate != null ? startDate : LocalDate.now().plusDays(7);

        validateSalaryTypeAndValues(finalSalaryType, baseSalaryPerHour, baseSalaryMonthly);

        EmploymentInvitation invitation = new EmploymentInvitation();
        invitation.setCompany(company);
        invitation.setCandidate(candidate);
        invitation.setApplication(application);
        invitation.setInvitationType("RECRUITMENT");
        invitation.setStatus("PENDING");
        invitation.setPosition(finalPosition);
        invitation.setSalaryType(finalSalaryType);
        invitation.setBaseSalaryPerHour(baseSalaryPerHour);
        invitation.setBaseSalaryMonthly(baseSalaryMonthly);
        invitation.setSalaryNote(salaryNote);
        invitation.setStartDate(finalStartDate);
        invitation.setNote(note);
        invitation.setInvitedBy(employerUser);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        EmploymentInvitation saved = invitationRepository.save(invitation);

        // Gửi thông báo đến Candidate
        Notification notification = new Notification();
        notification.setUser(candidate);
        notification.setTitle("Lời mời nhận việc từ tin tuyển dụng");
        notification.setMessage("Chúc mừng bạn! Hồ sơ ứng tuyển vị trí '" + application.getJob().getTitle() +
                "' tại " + company.getCompanyName() + " đã trúng tuyển. Lời mời nhận việc đã được gửi đến bạn.");
        notificationRepository.save(notification);

        return saved;
    }

    private void checkInvitationRateLimit(Integer employerUserId, Integer companyId) {
        String key = employerUserId + ":" + companyId;
        long now = System.currentTimeMillis();
        long windowMillis = 10_000L; // 10 giây
        int maxRequests = 5; // Tối đa 5 lời mời trong 10 giây cho mỗi tài khoản quản trị

        java.util.concurrent.ConcurrentLinkedQueue<Long> queue = invitationRateLimits.computeIfAbsent(key, k -> new java.util.concurrent.ConcurrentLinkedQueue<>());
        while (!queue.isEmpty() && now - queue.peek() > windowMillis) {
            queue.poll();
        }
        if (queue.size() >= maxRequests) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bạn đang gửi lời mời quá nhanh. Vui lòng thử lại sau vài giây.");
        }
        queue.add(now);
    }

    @Transactional
    public EmploymentInvitationResponse createRecruitmentInvitationFromApplication(
            Integer employerUserId,
            Integer companyId,
            Integer applicationId,
            RecruitmentInvitationCreateRequest request
    ) {
        // 0. Chống DDOS / Spam thao tác và chống gửi đồng thời cùng 1 hồ sơ
        checkInvitationRateLimit(employerUserId, companyId);
        Long prev = inFlightApplications.putIfAbsent(applicationId, System.currentTimeMillis());
        if (prev != null && (System.currentTimeMillis() - prev < 5000L)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lời mời cho hồ sơ này đang được xử lý, vui lòng không gửi liên tục.");
        }

        try {
            return doCreateRecruitmentInvitationFromApplication(employerUserId, companyId, applicationId, request);
        } finally {
            inFlightApplications.remove(applicationId);
        }
    }

    private EmploymentInvitationResponse doCreateRecruitmentInvitationFromApplication(
            Integer employerUserId,
            Integer companyId,
            Integer applicationId,
            RecruitmentInvitationCreateRequest request
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Công ty không tồn tại"));

        // 1. Phân quyền chặt chẽ: Chỉ COMPANY_ADMIN của đúng công ty mới có quyền gửi lời mời tuyển dụng
        boolean isAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(companyId, employerUserId, List.of("COMPANY_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên công ty (COMPANY_ADMIN) mới có quyền gửi lời mời tuyển dụng");
        }

        // 2. Tìm hồ sơ ứng tuyển
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hồ sơ ứng tuyển không tồn tại"));

        // 3. Kiểm tra Application thuộc đúng công ty
        if (application.getJob() == null || application.getJob().getCompany() == null || !application.getJob().getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hồ sơ ứng tuyển không thuộc công ty của bạn");
        }

        // 4. Kiểm tra Application.status = ACCEPTED
        if (!"ACCEPTED".equalsIgnoreCase(application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể gửi lời mời tuyển dụng cho hồ sơ đã được duyệt (ACCEPTED)");
        }

        User candidate = application.getCandidate();
        if (candidate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ ứng tuyển không có thông tin ứng viên hợp lệ");
        }

        // 5. Kiểm tra toàn bộ trạng thái lời mời của Application này:
        // Không phát hành lời mời mới nếu đã có lời mời PENDING hoặc ACCEPTED
        boolean hasActiveInvitation = invitationRepository.existsByApplicationIdAndStatusIn(applicationId, List.of("PENDING", "ACCEPTED"));
        if (hasActiveInvitation) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ này đã có lời mời làm việc đang chờ phản hồi hoặc đã được chấp thuận");
        }

        // 6. Kiểm tra Employment ACTIVE hoặc PENDING_START của ứng viên tại công ty
        boolean hasActiveEmployment = employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(
                companyId, candidate.getId(), List.of("ACTIVE", "PENDING_START")
        );
        if (hasActiveEmployment) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ứng viên đã là nhân viên đang hoạt động hoặc chuẩn bị nhận việc tại công ty");
        }

        // 7. Chống lời mời PENDING trùng lặp của cùng ứng viên tại công ty
        boolean hasPendingCandidateInvite = invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(companyId, candidate.getId(), "PENDING");
        if (hasPendingCandidateInvite) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ứng viên đang có một lời mời làm việc khác ở trạng thái PENDING tại công ty");
        }

        User employerUser = userRepository.findById(employerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng người tuyển dụng không tồn tại"));

        String finalPosition = (request.getPosition() != null && !request.getPosition().isBlank())
                ? request.getPosition().trim()
                : (application.getJob() != null ? application.getJob().getTitle() : "Nhân viên");

        String finalSalaryType = (request.getSalaryType() != null && !request.getSalaryType().isBlank())
                ? request.getSalaryType().toUpperCase().trim()
                : "HOURLY";

        LocalDate finalStartDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().plusDays(7);
        if (finalStartDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu làm việc không thể ở quá khứ");
        }

        validateSalaryTypeAndValues(finalSalaryType, request.getBaseSalaryPerHour(), request.getBaseSalaryMonthly());

        EmploymentInvitation invitation = new EmploymentInvitation();
        invitation.setCompany(company);
        invitation.setCandidate(candidate);
        invitation.setApplication(application);
        invitation.setInvitationType("RECRUITMENT");
        invitation.setStatus("PENDING");
        invitation.setPosition(finalPosition);
        invitation.setSalaryType(finalSalaryType);
        invitation.setBaseSalaryPerHour(request.getBaseSalaryPerHour());
        invitation.setBaseSalaryMonthly(request.getBaseSalaryMonthly());
        invitation.setSalaryNote(request.getSalaryNote());
        invitation.setStartDate(finalStartDate);
        invitation.setNote(request.getNote());
        invitation.setInvitedBy(employerUser);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        EmploymentInvitation saved;
        try {
            saved = invitationRepository.save(invitation);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lời mời làm việc cho hồ sơ này đã được tạo hoặc đang được xử lý đồng thời");
        }

        // 8. Gửi thông báo đến Candidate (Chỉ khi tạo thành công)
        Notification notification = new Notification();
        notification.setUser(candidate);
        notification.setTitle("Lời mời nhận việc từ tin tuyển dụng");
        notification.setMessage("Chúc mừng bạn! Công ty " + company.getCompanyName() + " đã gửi lời mời nhận việc cho hồ sơ ứng tuyển vị trí '" +
                (application.getJob() != null ? application.getJob().getTitle() : finalPosition) + "'. Vui lòng kiểm tra và phản hồi.");
        notificationRepository.save(notification);

        return buildInvitationResponse(saved);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public EmploymentInvitationResponse cancelInvitation(Integer employerUserId, Integer companyId, Integer invitationId) {
        // Chỉ COMPANY_ADMIN mới có quyền hủy lời mời
        boolean isAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(companyId, employerUserId, List.of("COMPANY_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên công ty (COMPANY_ADMIN) mới có quyền hủy lời mời");
        }

        EmploymentInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lời mời không tồn tại"));

        if (!invitation.getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lời mời này không thuộc công ty của bạn");
        }

        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hủy lời mời khi đang ở trạng thái PENDING");
        }

        LocalDateTime now = LocalDateTime.now();
        invitationRepository.cancelAtomic(invitationId, companyId, now);
        invitation.setStatus("CANCELLED");
        invitation.setRespondedAt(now);
        EmploymentInvitation saved = invitationRepository.save(invitation);

        // Thông báo đến Candidate (chỉ khi cập nhật thành công!)
        Notification notification = new Notification();
        notification.setUser(saved.getCandidate());
        notification.setTitle("Lời mời làm việc đã bị thu hồi");
        notification.setMessage("Lời mời làm việc vị trí '" + saved.getPosition() +
                "' tại " + saved.getCompany().getCompanyName() + " đã được thu hồi.");
        notificationRepository.save(notification);

        return buildInvitationResponse(saved);
    }

    public Page<EmploymentInvitationResponse> getCompanyInvitations(Integer employerUserId, Integer companyId, String status, int page, int size) {
        boolean isMember = memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(companyId, employerUserId, List.of("COMPANY_ADMIN", "RECRUITER"));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập lời mời của công ty này");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EmploymentInvitation> pageResult;
        if (status != null && !status.isBlank()) {
            pageResult = invitationRepository.findByCompanyIdAndStatus(companyId, status.toUpperCase().trim(), pageable);
        } else {
            pageResult = invitationRepository.findByCompanyId(companyId, pageable);
        }

        return pageResult.map(this::buildInvitationResponse);
    }

    public Page<EmploymentInvitationResponse> getCandidateInvitations(Integer candidateId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EmploymentInvitation> pageResult;
        if (status != null && !status.isBlank()) {
            pageResult = invitationRepository.findByCandidateIdAndStatus(candidateId, status.toUpperCase().trim(), pageable);
        } else {
            pageResult = invitationRepository.findByCandidateId(candidateId, pageable);
        }

        pageResult.forEach(this::checkAndExpireInvitation);
        return pageResult.map(this::buildInvitationResponse);
    }

    public EmploymentInvitationResponse getCandidateInvitationDetail(Integer candidateId, Integer invitationId) {
        EmploymentInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lời mời không tồn tại"));

        if (!invitation.getCandidate().getId().equals(candidateId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem lời mời này");
        }

        checkAndExpireInvitation(invitation);
        return buildInvitationResponse(invitation);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public EmploymentInvitationResponse acceptInvitation(Integer candidateId, Integer invitationId) {
        EmploymentInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lời mời không tồn tại"));

        if (!invitation.getCandidate().getId().equals(candidateId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền phản hồi lời mời này");
        }

        if (checkAndExpireInvitation(invitation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lời mời đã hết hạn phản hồi");
        }

        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể chấp thuận lời mời khi đang ở trạng thái PENDING (Trạng thái hiện tại: " + invitation.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        invitationRepository.acceptAtomic(invitationId, candidateId, now);
        invitation.setStatus("ACCEPTED");
        invitation.setRespondedAt(now);
        EmploymentInvitation saved = invitationRepository.save(invitation);

        // Gửi thông báo đến Employer (chỉ khi cập nhật thành công!)
        if (saved.getInvitedBy() != null) {
            Notification notification = new Notification();
            notification.setUser(saved.getInvitedBy());
            notification.setTitle("Ứng viên đã chấp nhận lời mời");
            notification.setMessage("Ứng viên " + saved.getCandidate().getEmail() +
                    " đã chấp thuận lời mời vào vị trí '" + saved.getPosition() +
                    "'. Hãy tiến hành Onboarding để kích hoạt hợp đồng nhân sự.");
            notificationRepository.save(notification);
        }

        // BẢO ĐẢM: Chưa tự động tạo Employment tại đây (chờ Employer Onboarding)
        return buildInvitationResponse(saved);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public EmploymentInvitationResponse declineInvitation(Integer candidateId, Integer invitationId, String declineReason) {
        EmploymentInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lời mời không tồn tại"));

        if (!invitation.getCandidate().getId().equals(candidateId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền phản hồi lời mời này");
        }

        if (checkAndExpireInvitation(invitation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lời mời đã hết hạn phản hồi");
        }

        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể từ chối lời mời khi đang ở trạng thái PENDING (Trạng thái hiện tại: " + invitation.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = (declineReason != null && !declineReason.isBlank()) ? declineReason.trim() : null;
        invitationRepository.declineAtomic(invitationId, candidateId, reason, now);
        invitation.setStatus("DECLINED");
        invitation.setDeclineReason(reason);
        invitation.setRespondedAt(now);
        EmploymentInvitation saved = invitationRepository.save(invitation);

        // Gửi thông báo đến Employer (chỉ khi cập nhật thành công!)
        if (saved.getInvitedBy() != null) {
            Notification notification = new Notification();
            notification.setUser(saved.getInvitedBy());
            notification.setTitle("Ứng viên đã từ chối lời mời");
            notification.setMessage("Ứng viên " + saved.getCandidate().getEmail() +
                    " đã từ chối lời mời vào vị trí '" + saved.getPosition() + "'" +
                    (saved.getDeclineReason() != null ? " với lý do: " + saved.getDeclineReason() : "."));
            notificationRepository.save(notification);
        }

        return buildInvitationResponse(saved);
    }

    @Transactional
    public EmployeeResponse onboardCandidate(Integer employerUserId, Integer companyId, Integer invitationId) {
        // 1. Phân quyền: Chỉ COMPANY_ADMIN mới có quyền onboard
        boolean isAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(companyId, employerUserId, List.of("COMPANY_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên công ty (COMPANY_ADMIN) mới có quyền hoàn tất Onboarding");
        }

        EmploymentInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lời mời không tồn tại"));

        // 2. Kiểm tra thuộc đúng công ty
        if (!invitation.getCompany().getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lời mời này không thuộc công ty của bạn");
        }

        // 3. Công ty phải bật HRM
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Công ty không tồn tại"));
        if (!Boolean.TRUE.equals(company.getHrmEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Công ty chưa kích hoạt tính năng Quản lý nhân sự (HRM)");
        }

        // 4. Chỉ chấp nhận invitation.status = ACCEPTED
        if (!"ACCEPTED".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể hoàn tất Onboarding khi ứng viên đã chấp thuận lời mời (ACCEPTED)");
        }

        // 5. Chặn Onboarding trùng lặp từ cùng 1 lời mời
        if (employmentRepository.existsByInvitationId(invitation.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lời mời này đã được hoàn tất Onboarding trước đó");
        }

        // 6. Chống trùng Employment ACTIVE hoặc PENDING_START tại công ty này
        if (employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(
                companyId, invitation.getCandidate().getId(), List.of("ACTIVE", "PENDING_START")
        )) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ứng viên đã có hợp đồng nhân sự đang hoạt động hoặc chuẩn bị bắt đầu tại công ty này");
        }

        // 7. Bảo toàn bất biến các điều khoản đã được Candidate chấp nhận
        LocalDate startDate = invitation.getStartDate() != null ? invitation.getStartDate() : LocalDate.now();
        String empStatus = startDate.isAfter(LocalDate.now()) ? "PENDING_START" : "ACTIVE";

        Employment employment = new Employment();
        employment.setCompany(company);
        employment.setCandidate(invitation.getCandidate());
        employment.setInvitation(invitation);
        employment.setPosition(invitation.getPosition());
        employment.setSalaryType(invitation.getSalaryType());
        employment.setBaseSalaryPerHour(invitation.getBaseSalaryPerHour());
        employment.setBaseSalaryMonthly(invitation.getBaseSalaryMonthly());
        employment.setStartDate(startDate);
        employment.setStatus(empStatus);

        Employment saved;
        try {
            saved = employmentRepository.save(employment);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ứng viên đã có hợp đồng nhân sự đang hoạt động hoặc chuẩn bị bắt đầu tại công ty này");
        }

        // 8. Thông báo đến Candidate
        Notification notification = new Notification();
        notification.setUser(invitation.getCandidate());
        notification.setTitle("Hoàn tất tiếp nhận nhân sự");
        notification.setMessage("Chúc mừng bạn! Công ty " + company.getCompanyName() +
                " đã hoàn tất tiếp nhận bạn vào vị trí '" + saved.getPosition() +
                "' (Trạng thái: " + saved.getStatus() + ").");
        notificationRepository.save(notification);

        CandidateProfile candidateProfile = candidateProfileRepository.findById(saved.getCandidate().getId()).orElse(null);
        String candidateName = candidateProfile != null && candidateProfile.getFullName() != null && !candidateProfile.getFullName().isBlank()
                ? candidateProfile.getFullName()
                : saved.getCandidate().getEmail();

        return EmployeeResponse.builder()
                .employmentId(saved.getId())
                .studentId(saved.getCandidate().getId())
                .studentName(candidateName)
                .email(saved.getCandidate().getEmail())
                .phone(candidateProfile != null ? candidateProfile.getPhoneNumber() : null)
                .universityName(candidateProfile != null ? candidateProfile.getUniversityName() : null)
                .major(candidateProfile != null ? candidateProfile.getMajor() : null)
                .baseSalaryPerHour(saved.getBaseSalaryPerHour())
                .baseSalaryMonthly(saved.getBaseSalaryMonthly())
                .salaryType(saved.getSalaryType())
                .position(saved.getPosition())
                .invitationId(saved.getInvitation() != null ? saved.getInvitation().getId() : null)
                .companyId(saved.getCompany().getId())
                .companyName(saved.getCompany().getCompanyName())
                .startDate(saved.getStartDate())
                .status(saved.getStatus())
                .build();
    }

    @Transactional
    public int activateDueEmployments() {
        List<Employment> dueEmployments = employmentRepository.findByStatusAndStartDateLessThanEqual("PENDING_START", LocalDate.now());
        for (Employment emp : dueEmployments) {
            emp.setStatus("ACTIVE");
            employmentRepository.save(emp);
        }
        return dueEmployments.size();
    }

    public List<CandidateEmploymentResponse> getCandidateEmployments(Integer candidateId) {
        activateDueEmployments();
        List<Employment> employments = employmentRepository.findByCandidateIdAndStatusIn(
                candidateId, List.of("ACTIVE", "PENDING_START")
        );

        List<CandidateEmploymentResponse> responses = new java.util.ArrayList<>();

        // 1. Existing Active/Pending Employments
        for (Employment emp : employments) {
            responses.add(CandidateEmploymentResponse.builder()
                    .employmentId(emp.getId())
                    .invitationId(emp.getInvitation() != null ? emp.getInvitation().getId() : null)
                    .isEmployment(true)
                    .companyId(emp.getCompany().getId())
                    .companyName(emp.getCompany().getCompanyName())
                    .companyLogoUrl(emp.getCompany().getLogoUrl())
                    .hrmEnabled(Boolean.TRUE.equals(emp.getCompany().getHrmEnabled()))
                    .position(emp.getPosition())
                    .salaryType(emp.getSalaryType())
                    .baseSalaryPerHour(emp.getBaseSalaryPerHour())
                    .baseSalaryMonthly(emp.getBaseSalaryMonthly())
                    .startDate(emp.getStartDate())
                    .status(emp.getStatus())
                    .build());
        }

        // 2. Accepted invitations that do not have an employment yet (e.g. company hasn't enabled HRM or hasn't onboarded yet)
        List<EmploymentInvitation> acceptedInvitations = invitationRepository.findByCandidateIdAndStatus(candidateId, "ACCEPTED");
        for (EmploymentInvitation inv : acceptedInvitations) {
            boolean alreadyHasEmployment = responses.stream()
                    .anyMatch(r -> r.getInvitationId() != null && r.getInvitationId().equals(inv.getId()));
            if (!alreadyHasEmployment && !employmentRepository.existsByInvitationId(inv.getId())) {
                responses.add(CandidateEmploymentResponse.builder()
                        .employmentId(null)
                        .invitationId(inv.getId())
                        .isEmployment(false)
                        .companyId(inv.getCompany().getId())
                        .companyName(inv.getCompany().getCompanyName())
                        .companyLogoUrl(inv.getCompany().getLogoUrl())
                        .hrmEnabled(Boolean.TRUE.equals(inv.getCompany().getHrmEnabled()))
                        .position(inv.getPosition())
                        .salaryType(inv.getSalaryType())
                        .baseSalaryPerHour(inv.getBaseSalaryPerHour())
                        .baseSalaryMonthly(inv.getBaseSalaryMonthly())
                        .startDate(inv.getStartDate())
                        .status("ACCEPTED_OFFER")
                        .build());
            }
        }

        return responses;
    }

    public List<EmployeeResponse> getCompanyEmployees(Integer employerUserId, Integer companyId) {
        boolean isMember = memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(companyId, employerUserId, List.of("COMPANY_ADMIN", "RECRUITER"));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu nhân sự của công ty này");
        }

        activateDueEmployments();
        List<Employment> employments = employmentRepository.findByCompanyId(companyId);

        return employments.stream().map(emp -> {
            CandidateProfile candidate = candidateProfileRepository.findById(emp.getCandidate().getId()).orElse(null);
            String candidateName = candidate != null && candidate.getFullName() != null && !candidate.getFullName().isBlank()
                    ? candidate.getFullName()
                    : emp.getCandidate().getEmail();

            return EmployeeResponse.builder()
                    .employmentId(emp.getId())
                    .studentId(emp.getCandidate().getId())
                    .studentName(candidateName)
                    .email(emp.getCandidate().getEmail())
                    .phone(candidate != null ? candidate.getPhoneNumber() : null)
                    .universityName(candidate != null ? candidate.getUniversityName() : null)
                    .major(candidate != null ? candidate.getMajor() : null)
                    .baseSalaryPerHour(emp.getBaseSalaryPerHour())
                    .baseSalaryMonthly(emp.getBaseSalaryMonthly())
                    .salaryType(emp.getSalaryType())
                    .position(emp.getPosition())
                    .invitationId(emp.getInvitation() != null ? emp.getInvitation().getId() : null)
                    .companyId(emp.getCompany().getId())
                    .companyName(emp.getCompany().getCompanyName())
                    .startDate(emp.getStartDate())
                    .status(emp.getStatus())
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    public boolean checkAndExpireInvitation(EmploymentInvitation invitation) {
        if ("PENDING".equalsIgnoreCase(invitation.getStatus()) &&
                invitation.getExpiresAt() != null &&
                invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("EXPIRED");
            try {
                invitationRepository.expireAtomic(invitation.getId(), LocalDateTime.now());
            } catch (Exception ignored) {}
            invitationRepository.save(invitation);
            return true;
        }
        return "EXPIRED".equalsIgnoreCase(invitation.getStatus());
    }

    private void validateSalaryTypeAndValues(String salaryType, BigDecimal baseSalaryPerHour, BigDecimal baseSalaryMonthly) {
        if (!VALID_SALARY_TYPES.contains(salaryType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại hình lương không hợp lệ. Các loại hỗ trợ: HOURLY, MONTHLY, NEGOTIABLE, UNPAID");
        }

        if ("HOURLY".equals(salaryType)) {
            if (baseSalaryPerHour == null || baseSalaryPerHour.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức lương theo giờ (baseSalaryPerHour) phải lớn hơn 0");
            }
        } else if ("MONTHLY".equals(salaryType)) {
            if (baseSalaryMonthly == null || baseSalaryMonthly.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mức lương theo tháng (baseSalaryMonthly) phải lớn hơn 0");
            }
        }
    }

    public EmploymentInvitationResponse buildInvitationResponse(EmploymentInvitation inv) {
        CandidateProfile candidateProfile = candidateProfileRepository.findById(inv.getCandidate().getId()).orElse(null);
        String candidateName = candidateProfile != null && candidateProfile.getFullName() != null && !candidateProfile.getFullName().isBlank()
                ? candidateProfile.getFullName()
                : inv.getCandidate().getEmail();

        EmployerProfile employerProfile = (inv.getInvitedBy() != null)
                ? employerProfileRepository.findById(inv.getInvitedBy().getId()).orElse(null)
                : null;
        String invitedByName = employerProfile != null && employerProfile.getFullName() != null && !employerProfile.getFullName().isBlank()
                ? employerProfile.getFullName()
                : (inv.getInvitedBy() != null ? inv.getInvitedBy().getEmail() : "Hệ thống");

        Integer invitedById = inv.getInvitedBy() != null ? inv.getInvitedBy().getId() : null;

        return EmploymentInvitationResponse.builder()
                .id(inv.getId())
                .companyId(inv.getCompany().getId())
                .companyName(inv.getCompany().getCompanyName())
                .companyLogoUrl(inv.getCompany().getLogoUrl())
                .candidateId(inv.getCandidate().getId())
                .candidateName(candidateName)
                .candidateEmail(inv.getCandidate().getEmail())
                .applicationId(inv.getApplication() != null ? inv.getApplication().getId() : null)
                .invitationType(inv.getInvitationType())
                .status(inv.getStatus())
                .position(inv.getPosition())
                .salaryType(inv.getSalaryType())
                .baseSalaryPerHour(inv.getBaseSalaryPerHour())
                .baseSalaryMonthly(inv.getBaseSalaryMonthly())
                .salaryNote(inv.getSalaryNote())
                .startDate(inv.getStartDate())
                .note(inv.getNote())
                .declineReason(inv.getDeclineReason())
                .invitedById(invitedById)
                .invitedByName(invitedByName)
                .createdAt(inv.getCreatedAt())
                .respondedAt(inv.getRespondedAt())
                .expiresAt(inv.getExpiresAt())
                .build();
    }
}
