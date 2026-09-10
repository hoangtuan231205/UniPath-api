package com.example.unipathapi.service;
import com.example.unipathapi.company.entity.*;
import com.example.unipathapi.company.repository.*;
import com.example.unipathapi.company.dto.request.*;
import com.example.unipathapi.company.dto.response.*;
import com.example.unipathapi.company.service.*;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;

import com.example.unipathapi.dto.request.ApplicationRequest;
import com.example.unipathapi.dto.request.UpdateApplicationStatusRequest;
import com.example.unipathapi.dto.response.ApplicationResponse;
import com.example.unipathapi.dto.response.CheckAppliedResponse;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
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
    private NotificationRepository notificationRepository;

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
        application.setCvProfileId(cvProfileId);
        application.setStatus("SUBMITTED");

        if (cvFile != null && !cvFile.isEmpty()) {
            if (cvFile.getSize() > 2 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước file CV không được vượt quá 2MB");
            }
            String originalFilename = cvFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            if (!extension.equals("pdf")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng file CV chỉ hỗ trợ PDF (.pdf)");
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
                application.setCvFileType("pdf");
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

    public Page<ApplicationResponse> getMyApplications(Integer candidateId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> pageResult = applicationRepository.findByCandidateIdAndOptionalStatus(candidateId, status, pageable);
        return pageResult.map(this::buildApplicationResponse);
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
            application.setInterviewAt(request.getInterviewAt());
            application.setInterviewLocation(request.getInterviewLocation().trim());
        }

        if (request.getNote() != null) {
            application.setNote(request.getNote().trim());
        }

        application.setStatus(newStatus);
        Application saved = applicationRepository.save(application);

        // Notify Candidate
        Notification notification = new Notification();
        notification.setUser(application.getCandidate());
        notification.setTitle("Cập nhật trạng thái ứng tuyển");
        notification.setMessage("Hồ sơ ứng tuyển vị trí '" + application.getJob().getTitle() +
                "' của bạn đã được chuyển sang trạng thái: " + newStatus +
                (request.getNote() != null ? " (Ghi chú: " + request.getNote() + ")" : ""));
        notificationRepository.save(notification);

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
            if (!"REVIEWING".equals(target) && !"REJECTED".equals(target)) {
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

    private ApplicationResponse buildApplicationResponse(Application app) {
        String candidateName = candidateProfileRepository.findById(app.getCandidate().getId())
                .map(CandidateProfile::getFullName).orElse(app.getCandidate().getEmail());

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .companyName(app.getJob().getCompany() != null ? app.getJob().getCompany().getCompanyName() : null)
                .candidateId(app.getCandidate().getId())
                .candidateName(candidateName)
                .candidateEmail(app.getCandidate().getEmail())
                .cvProfileId(app.getCvProfileId())
                .cvUrl(app.getCvUrl())
                .cvFilename(app.getCvFilename())
                .cvFileType(app.getCvFileType())
                .cvUploadedAt(app.getCvUploadedAt())
                .coverLetter(app.getCoverLetter())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .note(app.getNote())
                .interviewAt(app.getInterviewAt())
                .interviewLocation(app.getInterviewLocation())
                .build();
    }
}
