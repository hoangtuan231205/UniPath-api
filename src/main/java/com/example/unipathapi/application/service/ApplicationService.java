package com.example.unipathapi.application.service;
import com.example.unipathapi.notification.entity.Notification;
import com.example.unipathapi.notification.repository.NotificationRepository;
import com.example.unipathapi.application.entity.*;
import com.example.unipathapi.application.repository.*;
import com.example.unipathapi.application.dto.request.*;
import com.example.unipathapi.application.dto.response.*;
import com.example.unipathapi.application.service.*;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;
import com.example.unipathapi.company.entity.*;
import com.example.unipathapi.company.repository.*;
import com.example.unipathapi.company.dto.request.*;
import com.example.unipathapi.company.dto.response.*;
import com.example.unipathapi.company.service.*;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;

import com.example.unipathapi.application.dto.request.ApplicationRequest;
import com.example.unipathapi.application.dto.request.UpdateApplicationStatusRequest;
import com.example.unipathapi.application.dto.response.ApplicationDetailResponse;
import com.example.unipathapi.application.dto.response.ApplicationResponse;
import com.example.unipathapi.application.dto.response.CheckAppliedResponse;
import com.example.unipathapi.cv.entity.UserCvProfile;
import com.example.unipathapi.cv.repository.UserCvProfileRepository;
import com.example.unipathapi.company.service.EmploymentInvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    @Value("${app.storage.cv-path:./private-uploads/cv}")
    private String cvStoragePath;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyMemberRepository memberRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private UserCvProfileRepository userCvProfileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmploymentInvitationService employmentInvitationService;

    @Autowired
    private EmploymentInvitationRepository invitationRepository;

    @Autowired
    private EmploymentRepository employmentRepository;

    private static final Set<String> EMPLOYER_ALLOWED_STATUS = Set.of("REVIEWING", "INTERVIEW", "ACCEPTED", "REJECTED");

    public boolean isCompanyApplicationManager(Integer userId, Integer companyId) {
        if (userId == null || companyId == null) return false;
        return memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(
                companyId, userId, List.of("COMPANY_ADMIN", "RECRUITER")
        );
    }

    public CheckAppliedResponse checkApplied(Integer jobId, Integer candidateId) {
        Optional<Application> app = applicationRepository.findTopByJobIdAndCandidateIdOrderByAppliedAtDesc(jobId, candidateId);
        if (app.isPresent()) {
            return CheckAppliedResponse.builder()
                    .applied(true)
                    .appliedAt(app.get().getAppliedAt())
                    .status(app.get().getStatus())
                    .build();
        }
        return CheckAppliedResponse.builder()
                .applied(false)
                .build();
    }

    @Transactional
    public ApplicationResponse applyJob(Integer candidateId, Integer jobId, String coverLetter, Integer cvProfileId, MultipartFile cvFile) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));

        if (job.getIsActive() == null || !job.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin tuyển dụng này đã đóng hoặc chưa được duyệt");
        }

        if (job.getExpiredAt() != null && job.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin tuyển dụng này đã hết hạn ứng tuyển");
        }

        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã ứng tuyển vị trí này rồi");
        }

        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setCoverLetter(coverLetter);
        application.setStatus("SUBMITTED");

        if ((cvFile == null || cvFile.isEmpty()) && cvProfileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn hồ sơ CV Online hoặc tải lên tệp CV");
        }

        if (cvProfileId != null) {
            UserCvProfile cvProfile = userCvProfileRepository.findById(cvProfileId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ CV Online không tồn tại"));
            if (!cvProfile.getUser().getId().equals(candidateId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền sử dụng CV Online này");
            }
            application.setCvProfileId(cvProfileId);
            application.setCvSnapshotJson(cvProfile.getContentJson());
        }

        if (cvFile != null && !cvFile.isEmpty()) {
            if (cvFile.getSize() > 5 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước file CV không được vượt quá 5MB");
            }
            String originalFilename = cvFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            if (!java.util.Arrays.asList("pdf", "doc", "docx").contains(extension)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng file CV chỉ hỗ trợ PDF, DOC hoặc DOCX");
            }

            try {
                Path uploadPath = Paths.get(cvStoragePath);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String storedFilename = UUID.randomUUID() + "_" + originalFilename;
                Path filePath = uploadPath.resolve(storedFilename);
                Files.copy(cvFile.getInputStream(), filePath);

                application.setCvUrl(filePath.toString());
                application.setCvFilename(originalFilename);
                application.setCvFileType(extension.isBlank() ? "pdf" : extension);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi lưu file CV: " + e.getMessage());
            }
        }

        try {
            Application saved = applicationRepository.save(application);
            return buildApplicationResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã ứng tuyển vị trí này rồi");
        }
    }

    @Transactional
    public ApplicationResponse withdrawApplication(Integer applicationId, Integer candidateId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hồ sơ ứng tuyển không tồn tại"));

        if (!application.getCandidate().getId().equals(candidateId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền rút hồ sơ này");
        }

        String currentStatus = application.getStatus();
        if (!"SUBMITTED".equalsIgnoreCase(currentStatus) && !"REVIEWING".equalsIgnoreCase(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể rút hồ sơ khi trạng thái là SUBMITTED hoặc REVIEWING");
        }

        application.setStatus("WITHDRAWN");
        Application saved = applicationRepository.save(application);
        return buildApplicationResponse(saved);
    }

    public Page<ApplicationResponse> getMyApplications(Integer candidateId, String status, String search, String sort, int page, int size) {
        org.springframework.data.domain.Sort sortObj = org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "appliedAt"
        );
        if ("appliedAt,asc".equalsIgnoreCase(sort)) {
            sortObj = org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.ASC, "appliedAt"
            );
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Application> pageResult = applicationRepository.findCandidateApplications(candidateId, status, search, pageable);
        return pageResult.map(this::buildCandidateApplicationResponse);
    }

    public Page<ApplicationResponse> getMyApplications(Integer candidateId, String status, int page, int size) {
        return getMyApplications(candidateId, status, null, "appliedAt,desc", page, size);
    }

    public CandidateApplicationSummaryResponse getMyApplicationSummary(Integer candidateId) {
        List<Object[]> rows = applicationRepository.countByStatusForCandidate(candidateId);
        long total = 0;
        long submitted = 0;
        long reviewing = 0;
        long interview = 0;
        long accepted = 0;
        long rejected = 0;
        long withdrawn = 0;

        for (Object[] row : rows) {
            String st = row[0] != null ? row[0].toString().toUpperCase() : "";
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            total += count;
            switch (st) {
                case "SUBMITTED": submitted = count; break;
                case "REVIEWING": reviewing = count; break;
                case "INTERVIEW": interview = count; break;
                case "ACCEPTED": accepted = count; break;
                case "REJECTED": rejected = count; break;
                case "WITHDRAWN": withdrawn = count; break;
            }
        }

        return CandidateApplicationSummaryResponse.builder()
                .total(total)
                .submitted(submitted)
                .reviewing(reviewing)
                .interview(interview)
                .accepted(accepted)
                .rejected(rejected)
                .withdrawn(withdrawn)
                .build();
    }

    public Page<ApplicationResponse> getCompanyApplications(Integer currentUserId, Integer jobId, String status, int page, int size) {
        List<CompanyMember> memberships = memberRepository.findByUserId(currentUserId);
        List<Integer> companyIds = memberships.stream()
                .filter(m -> "COMPANY_ADMIN".equalsIgnoreCase(m.getMemberRole()) || "RECRUITER".equalsIgnoreCase(m.getMemberRole()))
                .map(m -> m.getCompany().getId())
                .collect(Collectors.toList());

        if (companyIds.isEmpty()) {
            return Page.empty();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Application> pageResult = applicationRepository.searchByCompanyIds(companyIds, jobId, status, pageable);
        return pageResult.map(this::buildApplicationResponse);
    }

    public Page<ApplicationResponse> getApplicationsByJobId(Integer jobId, Integer currentUserId, String status, int page, int size) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));

        Integer companyId = job.getCompany().getId();
        if (!isCompanyApplicationManager(currentUserId, companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý hồ sơ ứng tuyển của tin này");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Application> pageResult = applicationRepository.findByJobIdAndOptionalStatus(jobId, status, pageable);

        List<Integer> candidateUserIds = pageResult.getContent().stream()
                .map(a -> a.getCandidate() != null ? a.getCandidate().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, CandidateProfile> profileMap = candidateUserIds.isEmpty() ? Collections.emptyMap() :
                candidateProfileRepository.findAllById(candidateUserIds).stream()
                        .collect(Collectors.toMap(CandidateProfile::getId, p -> p));

        List<Integer> appIds = pageResult.getContent().stream()
                .map(Application::getId)
                .collect(Collectors.toList());

        Map<Integer, EmploymentInvitation> latestInviteMap = Collections.emptyMap();
        if (!appIds.isEmpty()) {
            List<EmploymentInvitation> invites = invitationRepository.findByApplicationIdIn(appIds);
            latestInviteMap = invites.stream()
                    .collect(Collectors.toMap(
                            i -> i.getApplication().getId(),
                            java.util.function.Function.identity(),
                            (oldVal, newVal) -> (newVal.getId() > oldVal.getId()) ? newVal : oldVal
                    ));
        }

        Set<Integer> activeCandidateIds = Collections.emptySet();
        if (!candidateUserIds.isEmpty()) {
            List<Employment> activeEmps = employmentRepository.findByCompanyIdAndCandidateIdInAndStatusIn(
                    companyId, candidateUserIds, List.of("ACTIVE", "PENDING_START")
            );
            activeCandidateIds = activeEmps.stream()
                    .map(e -> e.getCandidate().getId())
                    .collect(Collectors.toSet());
        }

        final Map<Integer, EmploymentInvitation> finalInviteMap = latestInviteMap;
        final Set<Integer> finalActiveCandidateIds = activeCandidateIds;

        return pageResult.map(app -> {
            Integer candId = app.getCandidate() != null ? app.getCandidate().getId() : null;
            return buildApplicationSummaryResponse(
                    app,
                    profileMap.get(candId),
                    finalInviteMap.get(app.getId()),
                    candId != null && finalActiveCandidateIds.contains(candId)
            );
        });
    }

    public ApplicationDetailResponse getApplicationDetail(Integer applicationId, Integer currentUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hồ sơ ứng tuyển không tồn tại"));

        Integer companyId = application.getJob().getCompany().getId();
        if (!isCompanyApplicationManager(currentUserId, companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hồ sơ của tin tuyển dụng này");
        }

        CandidateProfile profile = application.getCandidate() != null
                ? candidateProfileRepository.findById(application.getCandidate().getId()).orElse(null)
                : null;

        ApplicationDetailResponse.CandidateInfo candidateInfo = null;
        if (application.getCandidate() != null) {
            String fullName = profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()
                    ? profile.getFullName()
                    : application.getCandidate().getEmail();

            candidateInfo = ApplicationDetailResponse.CandidateInfo.builder()
                    .id(application.getCandidate().getId())
                    .email(application.getCandidate().getEmail())
                    .fullName(fullName)
                    .phoneNumber(profile != null ? profile.getPhoneNumber() : null)
                    .universityName(profile != null ? profile.getUniversityName() : null)
                    .major(profile != null ? profile.getMajor() : null)
                    .skills(profile != null ? profile.getSkills() : null)
                    .experienceYears(profile != null ? profile.getExperienceYears() : null)
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .build();
        }

        String snapshotJson = application.getCvSnapshotJson();
        boolean hasCvFile = application.getCvUrl() != null && !application.getCvUrl().isBlank();
        boolean hasCvOnline = application.getCvProfileId() != null || (snapshotJson != null && !snapshotJson.isBlank());
        boolean isCvOnlineAvailable = false;
        String cvOnlineStatusMessage = null;

        if (snapshotJson != null && !snapshotJson.isBlank()) {
            isCvOnlineAvailable = true;
        } else if (application.getCvProfileId() != null) {
            Optional<UserCvProfile> legacyCv = userCvProfileRepository.findById(application.getCvProfileId());
            if (legacyCv.isPresent()) {
                snapshotJson = legacyCv.get().getContentJson();
                isCvOnlineAvailable = true;
            } else {
                isCvOnlineAvailable = false;
                cvOnlineStatusMessage = "CV Online đã nộp trước đây hiện không còn khả dụng.";
            }
        }

        ApplicationDetailResponse.CvInfo cvInfo = ApplicationDetailResponse.CvInfo.builder()
                .hasCvFile(hasCvFile)
                .cvFilename(application.getCvFilename())
                .cvFileType(application.getCvFileType())
                .cvUploadedAt(application.getCvUploadedAt())
                .hasCvOnline(hasCvOnline)
                .cvProfileId(application.getCvProfileId())
                .cvSnapshotJson(snapshotJson)
                .isCvOnlineAvailable(isCvOnlineAvailable)
                .cvOnlineStatusMessage(cvOnlineStatusMessage)
                .build();

        Job job = application.getJob();
        Company company = job != null ? job.getCompany() : null;

        return ApplicationDetailResponse.builder()
                .id(application.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getCompanyName() : null)
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .coverLetter(application.getCoverLetter())
                .note(application.getNote())
                .interviewAt(application.getInterviewAt())
                .interviewLocation(application.getInterviewLocation())
                .candidate(candidateInfo)
                .cv(cvInfo)
                .build();
    }

    public Resource getCvPreviewResource(Integer applicationId, Integer currentUserId) {
        return getCvResource(applicationId, currentUserId);
    }


    @Transactional
    public ApplicationResponse updateApplicationStatus(Integer applicationId, Integer employerUserId, UpdateApplicationStatusRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hồ sơ ứng tuyển không tồn tại"));

        Integer companyId = application.getJob().getCompany().getId();
        if (!isCompanyApplicationManager(employerUserId, companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý hồ sơ của công ty này");
        }

        String newStatus = request.getStatus() != null ? request.getStatus().toUpperCase() : "";
        if (!EMPLOYER_ALLOWED_STATUS.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ cho thao tác của Nhà tuyển dụng");
        }

        String currentStatus = application.getStatus() != null ? application.getStatus().toUpperCase() : "SUBMITTED";

        // Validate Matrix
        validateStateTransition(currentStatus, newStatus);

        if ("INTERVIEW".equals(newStatus)) {
            if (request.getInterviewAt() == null || request.getInterviewLocation() == null || request.getInterviewLocation().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày giờ và địa điểm phỏng vấn là bắt buộc khi chuyển sang INTERVIEW");
            }
            if (request.getInterviewAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian phỏng vấn không thể ở quá khứ. Vui lòng chọn thời gian trong tương lai");
            }
            if (request.getInterviewAt().isAfter(LocalDateTime.now().plusDays(30))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian hẹn phỏng vấn không được vượt quá 30 ngày kể từ thời điểm hiện tại");
            }
            application.setInterviewAt(request.getInterviewAt());
            application.setInterviewLocation(request.getInterviewLocation().trim());
        }

        if (request.getNote() != null) {
            application.setNote(request.getNote().trim());
        }

        application.setStatus(newStatus);
        Application saved = applicationRepository.save(application);

        // Khi chuyển sang ACCEPTED: Chỉ cập nhật trạng thái hồ sơ, thông báo ứng viên chờ lời mời chính thức
        if ("ACCEPTED".equals(newStatus)) {
            Notification notification = new Notification();
            notification.setUser(application.getCandidate());
            notification.setTitle("Hồ sơ ứng tuyển đã được chấp thuận");
            notification.setMessage("Chúc mừng bạn! Hồ sơ ứng tuyển vị trí '" + application.getJob().getTitle() +
                    "' của bạn đã được nhà tuyển dụng chấp thuận (ACCEPTED). Vui lòng chờ nhà tuyển dụng gửi lời mời nhận việc chính thức.");
            notificationRepository.save(notification);
        } else {
            // Notify Candidate cho các trạng thái khác (REVIEWING, INTERVIEW, REJECTED)
            Notification notification = new Notification();
            notification.setUser(application.getCandidate());
            notification.setTitle("Cập nhật trạng thái ứng tuyển");
            notification.setMessage("Hồ sơ ứng tuyển vị trí '" + application.getJob().getTitle() +
                    "' của bạn đã được chuyển sang trạng thái: " + newStatus +
                    (request.getNote() != null ? " (Ghi chú: " + request.getNote() + ")" : ""));
            notificationRepository.save(notification);
        }

        return buildApplicationResponse(saved);
    }

    public Resource getCvResource(Integer applicationId, Integer currentUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hồ sơ ứng tuyển không tồn tại"));

        boolean isApplicant = application.getCandidate().getId().equals(currentUserId);
        boolean isManager = isCompanyApplicationManager(currentUserId, application.getJob().getCompany().getId());

        if (!isApplicant && !isManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tải file CV này");
        }

        if (application.getCvUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hồ sơ này không đính kèm file CV");
        }

        try {
            Path filePath = Paths.get(application.getCvUrl());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File CV không tồn tại trên hệ thống");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tải file CV: " + e.getMessage());
        }
    }

    public Application getApplicationById(Integer applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hồ sơ ứng tuyển không tồn tại"));
    }

    private void validateStateTransition(String current, String target) {
        if ("ACCEPTED".equals(current) || "REJECTED".equals(current) || "WITHDRAWN".equals(current)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển từ trạng thái cuối (" + current + ") sang trạng thái khác");
        }

        if ("SUBMITTED".equals(current)) {
            if (!"REVIEWING".equals(target) && !"INTERVIEW".equals(target) && !"REJECTED".equals(target)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển trực tiếp từ SUBMITTED sang " + target);
            }
        } else if ("REVIEWING".equals(current)) {
            if (!"INTERVIEW".equals(target) && !"REJECTED".equals(target)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển từ REVIEWING sang " + target);
            }
        } else if ("INTERVIEW".equals(current)) {
            if (!"ACCEPTED".equals(target) && !"REJECTED".equals(target)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển từ INTERVIEW sang " + target);
            }
        }
    }

    private ApplicationResponse buildCandidateApplicationResponse(Application app) {
        Job job = app.getJob();
        Company company = job != null ? job.getCompany() : null;
        CompanyLocation location = job != null ? job.getLocation() : null;

        String status = app.getStatus();
        boolean canWithdraw = "SUBMITTED".equalsIgnoreCase(status) || "REVIEWING".equalsIgnoreCase(status);

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getCompanyName() : null)
                .companyLogoUrl(company != null ? company.getLogoUrl() : null)
                .locationAddress(location != null ? location.getAddress() : null)
                .jobType(job != null ? job.getJobType() : null)
                .workMode(job != null ? job.getWorkMode() : null)
                .salaryRange(job != null ? job.getSalaryRange() : null)
                .cvProfileId(app.getCvProfileId())
                .cvFilename(app.getCvFilename())
                .cvFileType(app.getCvFileType())
                .cvUploadedAt(app.getCvUploadedAt())
                .coverLetter(app.getCoverLetter())
                .status(status)
                .appliedAt(app.getAppliedAt())
                .note(app.getNote())
                .interviewAt(app.getInterviewAt())
                .interviewLocation(app.getInterviewLocation())
                .canWithdraw(canWithdraw)
                .build();
    }

    private ApplicationResponse buildApplicationSummaryResponse(
            Application app,
            CandidateProfile profile,
            EmploymentInvitation invitation,
            Boolean hasActiveEmployment
    ) {
        String candidateName = profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()
                ? profile.getFullName()
                : (app.getCandidate() != null ? app.getCandidate().getEmail() : "Ứng viên");

        String status = app.getStatus();
        boolean canWithdraw = "SUBMITTED".equalsIgnoreCase(status) || "REVIEWING".equalsIgnoreCase(status);

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob() != null ? app.getJob().getId() : null)
                .candidateId(app.getCandidate() != null ? app.getCandidate().getId() : null)
                .candidateName(candidateName)
                .candidateEmail(app.getCandidate() != null ? app.getCandidate().getEmail() : null)
                .cvProfileId(app.getCvProfileId())
                .cvFilename(app.getCvFilename())
                .cvFileType(app.getCvFileType())
                .cvUploadedAt(app.getCvUploadedAt())
                .status(status)
                .appliedAt(app.getAppliedAt())
                .note(app.getNote())
                .interviewAt(app.getInterviewAt())
                .interviewLocation(app.getInterviewLocation())
                .canWithdraw(canWithdraw)
                .invitationId(invitation != null ? invitation.getId() : null)
                .invitationStatus(invitation != null ? invitation.getStatus() : null)
                .invitationSentAt(invitation != null ? invitation.getCreatedAt() : null)
                .hasActiveEmployment(Boolean.TRUE.equals(hasActiveEmployment))
                .build();
    }

    private ApplicationResponse buildApplicationSummaryResponse(Application app, CandidateProfile profile) {
        return buildApplicationSummaryResponse(app, profile, null, false);
    }

    private ApplicationResponse buildApplicationResponse(Application app) {
        String candidateName = candidateProfileRepository.findById(app.getCandidate().getId())
                .map(CandidateProfile::getFullName).orElse(app.getCandidate().getEmail());

        Job job = app.getJob();
        Company company = job != null ? job.getCompany() : null;
        CompanyLocation location = job != null ? job.getLocation() : null;

        String status = app.getStatus();
        boolean canWithdraw = "SUBMITTED".equalsIgnoreCase(status) || "REVIEWING".equalsIgnoreCase(status);

        EmploymentInvitation inv = invitationRepository.findTopByApplicationIdOrderByIdDesc(app.getId()).orElse(null);
        boolean hasActiveEmp = false;
        if (company != null && app.getCandidate() != null) {
            hasActiveEmp = employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(
                    company.getId(), app.getCandidate().getId(), List.of("ACTIVE", "PENDING_START")
            );
        }

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getCompanyName() : null)
                .companyLogoUrl(company != null ? company.getLogoUrl() : null)
                .locationAddress(location != null ? location.getAddress() : null)
                .jobType(job != null ? job.getJobType() : null)
                .workMode(job != null ? job.getWorkMode() : null)
                .salaryRange(job != null ? job.getSalaryRange() : null)
                .candidateId(app.getCandidate().getId())
                .candidateName(candidateName)
                .candidateEmail(app.getCandidate().getEmail())
                .cvProfileId(app.getCvProfileId())
                .cvFilename(app.getCvFilename())
                .cvFileType(app.getCvFileType())
                .cvUploadedAt(app.getCvUploadedAt())
                .coverLetter(app.getCoverLetter())
                .status(status)
                .appliedAt(app.getAppliedAt())
                .note(app.getNote())
                .interviewAt(app.getInterviewAt())
                .interviewLocation(app.getInterviewLocation())
                .canWithdraw(canWithdraw)
                .invitationId(inv != null ? inv.getId() : null)
                .invitationStatus(inv != null ? inv.getStatus() : null)
                .invitationSentAt(inv != null ? inv.getCreatedAt() : null)
                .hasActiveEmployment(hasActiveEmp)
                .build();
    }
}
