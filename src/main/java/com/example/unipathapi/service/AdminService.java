package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CategoryRequest;
import com.example.unipathapi.dto.request.ReportRequest;
import com.example.unipathapi.dto.request.ReportResolveRequest;
import com.example.unipathapi.dto.request.SkillRequest;
import com.example.unipathapi.dto.response.AdminStatsResponse;
import com.example.unipathapi.dto.response.AdminUserResponse;
import com.example.unipathapi.dto.response.ReportResponse;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobCategoryRepository categoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    // --- USERS MANAGEMENT ---
    public List<AdminUserResponse> getUsers(String type, Boolean status, String search) {
        List<User> users = userRepository.findAll();

        return users.stream()
                .filter(u -> {
                    if (type != null && !type.trim().isEmpty()) {
                        if (u.getRole() == null || !u.getRole().equalsIgnoreCase(type)) {
                            return false;
                        }
                    }
                    if (status != null) {
                        if (!status.equals(u.getIsActive())) {
                            return false;
                        }
                    }
                    if (search != null && !search.trim().isEmpty()) {
                        String s = search.toLowerCase();
                        if (!u.getEmail().toLowerCase().contains(s)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(this::buildAdminUserResponse)
                .collect(Collectors.toList());
    }

    public AdminUserResponse banUser(Integer targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Không thể khoá tài khoản có quyền ADMIN");
        }

        user.setIsActive(false);
        User saved = userRepository.save(user);
        return buildAdminUserResponse(saved);
    }

    public AdminUserResponse unbanUser(Integer targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        user.setIsActive(true);
        User saved = userRepository.save(user);
        return buildAdminUserResponse(saved);
    }

    // --- REPORTS ---
    public ReportResponse reportJob(Integer jobId, Integer reporterUserId, ReportRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Report report = new Report();
        report.setJob(job);
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setStatus("PENDING");

        Report saved = reportRepository.save(report);
        return buildReportResponse(saved);
    }

    public List<ReportResponse> getReports(String status) {
        List<Report> reports = reportRepository.findByOptionalStatus(status);
        return reports.stream().map(this::buildReportResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReportResponse resolveReport(Integer reportId, Integer adminUserId, ReportResolveRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Báo cáo không tồn tại"));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản admin không tồn tại"));

        String action = request.getAction();
        if ("REJECT".equalsIgnoreCase(action)) {
            report.setStatus("REJECTED");
        } else if ("DELETE_POST".equalsIgnoreCase(action) || "DELETE_JOB".equalsIgnoreCase(action)) {
            report.setStatus("RESOLVED");
            if (report.getJob() != null) {
                report.getJob().setIsActive(false);
                jobRepository.save(report.getJob());
            }
        } else if ("BAN_ACCOUNT".equalsIgnoreCase(action)) {
            report.setStatus("RESOLVED");
            if (report.getJob() != null && report.getJob().getCompany() != null) {
                User owner = report.getJob().getCompany().getUser();
                if (!"ADMIN".equalsIgnoreCase(owner.getRole())) {
                    owner.setIsActive(false);
                    userRepository.save(owner);
                }
            }
        } else {
            throw new RuntimeException("Hành động xử lý không hợp lệ");
        }

        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        Report saved = reportRepository.save(report);
        return buildReportResponse(saved);
    }

    // --- CATEGORIES CRUD ---
    public List<JobCategory> getCategories() {
        return categoryRepository.findAll();
    }

    public JobCategory createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }
        JobCategory category = new JobCategory();
        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    public JobCategory updateCategory(Integer id, CategoryRequest request) {
        JobCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    public void deleteCategory(Integer id) {
        JobCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        categoryRepository.delete(category);
    }

    // --- SKILLS CRUD ---
    public List<Skill> getSkills() {
        return skillRepository.findAll();
    }

    public Skill createSkill(SkillRequest request) {
        if (skillRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Tên kỹ năng đã tồn tại");
        }
        Skill skill = new Skill();
        skill.setName(request.getName());
        return skillRepository.save(skill);
    }

    public Skill updateSkill(Integer id, SkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kỹ năng không tồn tại"));
        skill.setName(request.getName());
        return skillRepository.save(skill);
    }

    public void deleteSkill(Integer id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kỹ năng không tồn tại"));

        if (jobSkillRepository.existsBySkillId(id)) {
            throw new RuntimeException("Kỹ năng đang được tham chiếu trong các tin tuyển dụng, không thể xoá");
        }
        skillRepository.delete(skill);
    }

    // --- STATS ---
    public AdminStatsResponse getStats() {
        long totalCandidates = candidateProfileRepository.count();
        long totalEmployers = employerProfileRepository.count();
        long activeJobs = jobRepository.countByIsActiveTrue();

        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("SUBMITTED", applicationRepository.countByStatus("SUBMITTED"));
        statusCounts.put("ACCEPTED", applicationRepository.countByStatus("ACCEPTED"));
        statusCounts.put("REJECTED", applicationRepository.countByStatus("REJECTED"));

        return AdminStatsResponse.builder()
                .totalCandidates(totalCandidates)
                .totalEmployers(totalEmployers)
                .activeJobs(activeJobs)
                .applicationStatusCounts(statusCounts)
                .build();
    }

    // --- HELPER METHODS ---
    private AdminUserResponse buildAdminUserResponse(User user) {
        String fullName = user.getEmail();
        if ("CANDIDATE".equalsIgnoreCase(user.getRole())) {
            fullName = candidateProfileRepository.findById(user.getId())
                    .map(CandidateProfile::getFullName).orElse(user.getEmail());
        } else if ("EMPLOYER".equalsIgnoreCase(user.getRole())) {
            fullName = employerProfileRepository.findById(user.getId())
                    .map(EmployerProfile::getFullName).orElse(user.getEmail());
        }

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .fullName(fullName)
                .build();
    }

    private ReportResponse buildReportResponse(Report r) {
        String resolverName = r.getResolvedBy() != null ? r.getResolvedBy().getEmail() : null;

        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterEmail(r.getReporter().getEmail())
                .jobId(r.getJob() != null ? r.getJob().getId() : null)
                .jobTitle(r.getJob() != null ? r.getJob().getTitle() : null)
                .reason(r.getReason())
                .status(r.getStatus())
                .resolvedById(r.getResolvedBy() != null ? r.getResolvedBy().getId() : null)
                .resolvedByName(resolverName)
                .resolvedAt(r.getResolvedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
