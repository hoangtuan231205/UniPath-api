package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.ApplicationRequest;
import com.example.unipathapi.dto.request.UpdateApplicationStatusRequest;
import com.example.unipathapi.dto.response.ApplicationResponse;
import com.example.unipathapi.dto.response.CheckAppliedResponse;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private static final String UPLOAD_DIR = "uploads/cvs/";

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    public CheckAppliedResponse checkApplied(Integer jobId, Integer studentId) {
        Optional<Application> app = applicationRepository.findTopByJobIdAndStudentIdOrderByAppliedAtDesc(jobId, studentId);
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
    public ApplicationResponse applyJob(Integer studentId, ApplicationRequest request, MultipartFile cvFile) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));

        if (!job.getIsActive()) {
            throw new RuntimeException("Tin tuyển dụng này đã đóng hoặc chưa được duyệt");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (applicationRepository.existsByJobIdAndStudentId(request.getJobId(), studentId)) {
            throw new RuntimeException("Bạn đã ứng tuyển vị trí này");
        }

        Application application = new Application();
        application.setJob(job);
        application.setStudent(student);
        application.setStatus("SUBMITTED");

        if (cvFile != null && !cvFile.isEmpty()) {
            if (cvFile.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("Kích thước file CV không được vượt quá 5MB");
            }
            String originalFilename = cvFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            if (!extension.equals("pdf") && !extension.equals("docx") && !extension.equals("doc")) {
                throw new RuntimeException("Định dạng file CV chỉ hỗ trợ PDF hoặc DOCX");
            }

            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String storedFilename = UUID.randomUUID() + "_" + originalFilename;
                Path filePath = uploadPath.resolve(storedFilename);
                Files.copy(cvFile.getInputStream(), filePath);

                application.setCvUrl("/" + UPLOAD_DIR + storedFilename);
                application.setCvFilename(originalFilename);
                application.setCvFileType(extension);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi lưu file CV: " + e.getMessage());
            }
        }

        Application saved = applicationRepository.save(application);
        return buildApplicationResponse(saved);
    }

    public void withdrawApplication(Integer applicationId, Integer studentId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Hồ sơ ứng tuyển không tồn tại"));

        if (!application.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Bạn không có quyền rút hồ sơ này");
        }

        if (!"SUBMITTED".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("Chỉ có thể rút hồ sơ khi trạng thái là SUBMITTED");
        }

        applicationRepository.delete(application);
    }

    public List<ApplicationResponse> getMyApplications(Integer studentId, String status) {
        List<Application> applications = applicationRepository.findByStudentIdAndOptionalStatus(studentId, status);
        return applications.stream().map(this::buildApplicationResponse).collect(Collectors.toList());
    }

    public List<ApplicationResponse> getJobApplications(Integer jobId, Integer employerUserId, String status) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));

        if (!job.getCompany().getUser().getId().equals(employerUserId)) {
            throw new RuntimeException("Bạn không có quyền xem danh sách ứng viên của tin tuyển dụng này");
        }

        List<Application> applications = applicationRepository.findByJobIdAndOptionalStatus(jobId, status);
        return applications.stream().map(this::buildApplicationResponse).collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Integer applicationId, Integer employerUserId, UpdateApplicationStatusRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Hồ sơ ứng tuyển không tồn tại"));

        if (!application.getJob().getCompany().getUser().getId().equals(employerUserId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật trạng thái hồ sơ này");
        }

        application.setStatus(request.getStatus());
        Application saved = applicationRepository.save(application);

        // Tạo thông báo cho candidate
        Notification notification = new Notification();
        notification.setUser(application.getStudent());
        notification.setTitle("Cập nhật trạng thái ứng tuyển");
        notification.setMessage("Hồ sơ ứng tuyển vị trí '" + application.getJob().getTitle() + 
                "' của bạn đã được chuyển sang trạng thái: " + request.getStatus() + 
                (request.getNote() != null ? " (Ghi chú: " + request.getNote() + ")" : ""));
        notificationRepository.save(notification);

        return buildApplicationResponse(saved);
    }

    public Resource getCvResource(Integer applicationId, Integer currentUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Hồ sơ ứng tuyển không tồn tại"));

        boolean isApplicant = application.getStudent().getId().equals(currentUserId);
        boolean isEmployer = application.getJob().getCompany().getUser().getId().equals(currentUserId);

        if (!isApplicant && !isEmployer) {
            throw new RuntimeException("Bạn không có quyền tải file CV này");
        }

        if (application.getCvUrl() == null) {
            throw new RuntimeException("Hồ sơ này không đính kèm file CV");
        }

        try {
            String filePathStr = application.getCvUrl().startsWith("/") ? application.getCvUrl().substring(1) : application.getCvUrl();
            Path filePath = Paths.get(filePathStr);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File CV không tồn tại trên hệ thống");
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tải file CV: " + e.getMessage());
        }
    }

    public Application getApplicationById(Integer applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Hồ sơ ứng tuyển không tồn tại"));
    }

    private ApplicationResponse buildApplicationResponse(Application app) {
        String studentName = candidateProfileRepository.findById(app.getStudent().getId())
                .map(CandidateProfile::getFullName).orElse(app.getStudent().getEmail());

        return ApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .companyName(app.getJob().getCompany() != null ? app.getJob().getCompany().getCompanyName() : null)
                .studentId(app.getStudent().getId())
                .studentName(studentName)
                .studentEmail(app.getStudent().getEmail())
                .cvUrl(app.getCvUrl())
                .cvFilename(app.getCvFilename())
                .cvFileType(app.getCvFileType())
                .cvUploadedAt(app.getCvUploadedAt())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .build();
    }
}
