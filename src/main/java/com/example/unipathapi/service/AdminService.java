package com.example.unipathapi.service;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;

import com.example.unipathapi.dto.request.AdminCreateUserRequest;
import com.example.unipathapi.dto.request.CompanyRequest;
import com.example.unipathapi.dto.request.CategoryRequest;
import com.example.unipathapi.dto.request.ReportRequest;
import com.example.unipathapi.dto.request.ReportResolveRequest;
import com.example.unipathapi.dto.request.SkillRequest;
import com.example.unipathapi.dto.response.*;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository memberRepository;

    @Autowired
    private CompanyJoinRequestRepository joinRequestRepository;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private AdminAuditLogService auditLogService;

    private static final Set<String> ALLOWED_REPORT_ACTIONS = Set.of("REJECT", "BAN_ACCOUNT", "DELETE_POST");

    // ==========================================
    // NHÓM 1.1: USER MANAGEMENT & MODERATION
    // ==========================================
        @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request, Integer adminUserId) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        String role = request.getRole() != null ? request.getRole().trim().toUpperCase() : "CANDIDATE";
        if (!Arrays.asList("ADMIN", "EMPLOYER", "CANDIDATE").contains(role)) {
            throw new RuntimeException("Vai trò không hợp lệ (chỉ chấp nhận ADMIN, EMPLOYER, CANDIDATE)");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(request.getPassword());
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        String fullName = request.getFullName() != null && !request.getFullName().trim().isEmpty()
                ? request.getFullName().trim()
                : email;

        if ("CANDIDATE".equals(role)) {
            CandidateProfile cp = new CandidateProfile();
            cp.setUser(savedUser);
            cp.setFullName(fullName);
            candidateProfileRepository.save(cp);
        } else if ("EMPLOYER".equals(role)) {
            EmployerProfile ep = new EmployerProfile();
            ep.setUser(savedUser);
            ep.setFullName(fullName);
            employerProfileRepository.save(ep);

            if (request.getCompanyId() != null) {
                Company comp = companyRepository.findById(request.getCompanyId()).orElse(null);
                if (comp != null) {
                    String memberRole = request.getCompanyRole() != null && !request.getCompanyRole().trim().isEmpty()
                            ? request.getCompanyRole().trim().toUpperCase()
                            : "COMPANY_ADMIN";

                    CompanyMember member = new CompanyMember();
                    member.setCompany(comp);
                    member.setUser(savedUser);
                    member.setMemberRole(memberRole);
                    member.setJoinedAt(LocalDateTime.now());
                    memberRepository.save(member);
                }
            }
        }

        // Audit Log
        String detail = "Tạo tài khoản: " + savedUser.getEmail() + " | Vai trò: " + role;
        if (request.getCompanyId() != null) {
            detail += " | Gán vào Công ty ID: #" + request.getCompanyId() + " (" + (request.getCompanyRole() != null ? request.getCompanyRole() : "COMPANY_ADMIN") + ")";
        }
        auditLogService.log(adminUserId, "CREATE_USER", "USER", savedUser.getId(), detail);

        return AdminUserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .isActive(savedUser.getIsActive())
                .createdAt(savedUser.getCreatedAt())
                .fullName(fullName)
                .build();
    }

    public List<AdminUserResponse> getUsers(String roleFilter, Boolean activeFilter, String searchKeyword, Integer page, Integer size) {
        List<User> users = userRepository.findAll();

        List<AdminUserResponse> filtered = users.stream()
                .filter(u -> roleFilter == null || roleFilter.isBlank() || roleFilter.equalsIgnoreCase(u.getRole()))
                .filter(u -> activeFilter == null || activeFilter.equals(u.getIsActive()))
                .filter(u -> {
                    if (searchKeyword == null || searchKeyword.isBlank()) return true;
                    String kw = searchKeyword.toLowerCase();
                    return u.getEmail().toLowerCase().contains(kw);
                })
                .map(this::buildAdminUserResponse)
                .collect(Collectors.toList());

        if (page != null && size != null && size > 0) {
            int fromIndex = Math.min(page * size, filtered.size());
            int toIndex = Math.min(fromIndex + size, filtered.size());
            return filtered.subList(fromIndex, toIndex);
        }
        return filtered;
    }

    @Transactional
    public AdminUserResponse banUser(Integer targetUserId, Integer adminUserId) {
        // Principle: Chặn Admin tự ban chính mình
        if (targetUserId.equals(adminUserId)) {
            throw new RuntimeException("Không thể tự khoá tài khoản của chính mình");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // Principle 1: Check active admin count if user is ADMIN
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            long activeAdminCount = userRepository.countByRoleAndIsActiveTrue("ADMIN");
            if (activeAdminCount <= 1 && Boolean.TRUE.equals(user.getIsActive())) {
                throw new RuntimeException("Không thể khóa tài khoản Admin duy nhất đang hoạt động");
            }
        }

        user.setIsActive(false);
        User saved = userRepository.save(user);

        // Principle: Audit log
        auditLogService.log(adminUserId, "BAN_USER", "USER", targetUserId, "Khóa tài khoản user: " + user.getEmail());

        return buildAdminUserResponse(saved);
    }

    @Transactional
    public AdminUserResponse unbanUser(Integer targetUserId, Integer adminUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        user.setIsActive(true);
        User saved = userRepository.save(user);

        // Principle: Audit log
        auditLogService.log(adminUserId, "UNBAN_USER", "USER", targetUserId, "Mở khóa tài khoản user: " + user.getEmail());

        return buildAdminUserResponse(saved);
    }

    // ==========================================
    // NHÓM 1.2: REPORT MODERATION (JOB & POST)
    // ==========================================
    @Transactional
    public ReportResponse reportJob(Integer jobId, Integer reporterId, ReportRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Tin tuyển dụng không tồn tại"));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Report report = new Report();
        report.setTargetType("JOB");
        report.setJob(job);
        report.setPost(null);
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setStatus("PENDING");

        Report saved = reportRepository.save(report);
        return buildReportResponse(saved);
    }

    @Transactional
    public ReportResponse reportPost(Integer postId, Integer reporterId, ReportRequest request) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Report report = new Report();
        report.setTargetType("POST");
        report.setPost(post);
        report.setJob(null);
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setStatus("PENDING");

        Report saved = reportRepository.save(report);
        return buildReportResponse(saved);
    }

    public List<ReportResponse> getReports(String targetType, String status, Integer page, Integer size) {
        String queryStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        String queryTargetType = (targetType != null && !targetType.isBlank()) ? targetType.trim().toUpperCase() : null;

        if (page != null && size != null && size > 0) {
            Pageable pageable = PageRequest.of(Math.max(page, 0), size);
            Page<Report> reportPage = reportRepository.findByOptionalFiltersPaged(queryTargetType, queryStatus, pageable);
            return reportPage.getContent().stream().map(this::buildReportResponse).collect(Collectors.toList());
        }

        List<Report> reports = reportRepository.findByOptionalFilters(queryTargetType, queryStatus);
        return reports.stream().map(this::buildReportResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReportResponse resolveReport(Integer reportId, Integer adminUserId, ReportResolveRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Báo cáo không tồn tại"));

        // Principle 6: Terminal status check
        if (!"PENDING".equalsIgnoreCase(report.getStatus())) {
            throw new RuntimeException("Báo cáo này đã được xử lý trước đó, không thể xử lý lại");
        }

        // Principle 4: Whitelist action check
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "";
        if (!ALLOWED_REPORT_ACTIONS.contains(action)) {
            throw new RuntimeException("Action xử lý báo cáo không hợp lệ. Chỉ chấp nhận REJECT, BAN_ACCOUNT, DELETE_POST");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản Admin không tồn tại"));

        if ("REJECT".equals(action)) {
            report.setStatus("REJECTED");
        } else if ("BAN_ACCOUNT".equals(action)) {
            if ("JOB".equalsIgnoreCase(report.getTargetType()) && report.getJob() != null) {
                // Determine accurate target user to ban: postedBy (if available) or company createdBy
                User targetUser = report.getJob().getPostedBy();
                if (targetUser == null && report.getJob().getCompany() != null) {
                    targetUser = report.getJob().getCompany().getCreatedBy();
                }
                if (targetUser != null) {
                    banUser(targetUser.getId(), adminUserId);
                }
            } else if ("POST".equalsIgnoreCase(report.getTargetType()) && report.getPost() != null) {
                User targetUser = report.getPost().getAuthor();
                if (targetUser != null) {
                    banUser(targetUser.getId(), adminUserId);
                }
            }
            report.setStatus("RESOLVED");
        } else if ("DELETE_POST".equals(action)) {
            if ("JOB".equalsIgnoreCase(report.getTargetType()) && report.getJob() != null) {
                // Soft delete Job (is_active = false)
                Job job = report.getJob();
                job.setIsActive(false);
                jobRepository.save(job);
            } else if ("POST".equalsIgnoreCase(report.getTargetType()) && report.getPost() != null) {
                // Soft delete Post (is_active = false)
                CommunityPost post = report.getPost();
                post.setIsActive(false);
                communityPostRepository.save(post);
            }
            report.setStatus("RESOLVED");
        }

        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        Report saved = reportRepository.save(report);

        // Audit log
        String targetDesc = "JOB".equalsIgnoreCase(report.getTargetType())
                ? "Job ID " + (report.getJob() != null ? report.getJob().getId() : "null")
                : "Post ID " + (report.getPost() != null ? report.getPost().getId() : "null");
        auditLogService.log(adminUserId, "RESOLVE_REPORT", "REPORT", reportId, "Xử lý báo cáo " + targetDesc + " với action: " + action);

        return buildReportResponse(saved);
    }

    // ==========================================
    // NHÓM 1.3: MASTER DATA CRUD
    // ==========================================
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

    public void deleteCategory(Integer id, Integer adminUserId) {
        JobCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        if (jobRepository.existsByCategoryId(id)) {
            throw new RuntimeException("Danh mục đang được sử dụng trong các tin tuyển dụng, không thể xóa");
        }
        categoryRepository.delete(category);
        auditLogService.log(adminUserId, "DELETE_CATEGORY", "JOB_CATEGORY", id, "Xóa danh mục: " + category.getName());
    }

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

    public void deleteSkill(Integer id, Integer adminUserId) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kỹ năng không tồn tại"));

        if (jobSkillRepository.existsBySkillId(id)) {
            throw new RuntimeException("Kỹ năng đang được tham chiếu trong các tin tuyển dụng, không thể xóa");
        }
        skillRepository.delete(skill);
        auditLogService.log(adminUserId, "DELETE_SKILL", "SKILL", id, "Xóa kỹ năng: " + skill.getName());
    }

    // ==========================================
    // NHÓM 1.4: STATS
    // ==========================================
    public AdminStatsResponse getStats() {
        long totalCandidates = userRepository.countByRole("CANDIDATE");
        long totalEmployers = userRepository.countByRole("EMPLOYER");
        long activeJobs = jobRepository.countByIsActiveTrue();

        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("SUBMITTED", applicationRepository.countByStatus("SUBMITTED"));
        statusCounts.put("REVIEWING", applicationRepository.countByStatus("REVIEWING"));
        statusCounts.put("INTERVIEW", applicationRepository.countByStatus("INTERVIEW"));
        statusCounts.put("ACCEPTED", applicationRepository.countByStatus("ACCEPTED"));
        statusCounts.put("REJECTED", applicationRepository.countByStatus("REJECTED"));
        statusCounts.put("WITHDRAWN", applicationRepository.countByStatus("WITHDRAWN"));

        return AdminStatsResponse.builder()
                .totalCandidates(totalCandidates)
                .totalEmployers(totalEmployers)
                .activeJobs(activeJobs)
                .applicationStatusCounts(statusCounts)
                .build();
    }

    // ==========================================
    // NHÓM 2.1: COMPANY APPROVALS
    // ==========================================
    public List<CompanyResponse> getCompaniesByStatus(String status) {
        String queryStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "PENDING";
        List<Company> companies = companyRepository.findByStatus(queryStatus);
        return companies.stream().map(this::buildCompanyResponse).collect(Collectors.toList());
    }

        @Transactional
    public CompanyResponse createCompanyByAdmin(CompanyRequest request, Integer adminUserId) {
        if (request.getTaxCode() != null && !request.getTaxCode().trim().isEmpty()) {
            if (companyRepository.findByTaxCode(request.getTaxCode().trim()).isPresent()) {
                throw new RuntimeException("Mã số thuế này đã tồn tại trong hệ thống");
            }
        }

        User adminUser = userRepository.findById(adminUserId).orElse(null);

        Company comp = new Company();
        comp.setCompanyName(request.getCompanyName().trim());
        comp.setCompanyScale(request.getCompanyScale() != null ? request.getCompanyScale().trim() : "SME");
        comp.setDescription(request.getDescription());
        comp.setWebsite(request.getWebsite());
        comp.setTaxCode(request.getTaxCode() != null && !request.getTaxCode().trim().isEmpty() ? request.getTaxCode().trim() : null);
        comp.setPhoneNumber(request.getPhoneNumber());
        comp.setBusinessLicenseUrl(request.getBusinessLicenseUrl());
        comp.setStatus("APPROVED");
        comp.setCreatedBy(adminUser);
        comp.setApprovedBy(adminUser);
        comp.setApprovedAt(LocalDateTime.now());

        Company saved = companyRepository.save(comp);

        auditLogService.log(adminUserId, "CREATE_COMPANY", "COMPANY", saved.getId(),
                "Super Admin tạo trực tiếp doanh nghiệp: " + saved.getCompanyName() + " (MST: " + saved.getTaxCode() + ")");

        return buildCompanyResponse(saved);
    }

    @Transactional
    public CompanyResponse approveCompanyProposal(Integer companyId, Integer adminUserId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Công ty không tồn tại"));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản admin không tồn tại"));

        company.setStatus("APPROVED");
        company.setApprovedBy(admin);
        company.setApprovedAt(LocalDateTime.now());

        if (company.getCreatedBy() != null) {
            if (!memberRepository.existsByCompanyIdAndUserId(company.getId(), company.getCreatedBy().getId())) {
                CompanyMember companyAdmin = new CompanyMember(company, company.getCreatedBy(), "COMPANY_ADMIN");
                memberRepository.save(companyAdmin);
            }
        }

        Company saved = companyRepository.save(company);
        auditLogService.log(adminUserId, "APPROVE_COMPANY", "COMPANY", companyId, "Duyệt công ty: " + company.getCompanyName());
        return buildCompanyResponse(saved);
    }

    @Transactional
    public CompanyResponse rejectCompanyProposal(Integer companyId, Integer adminUserId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Công ty không tồn tại"));

        company.setStatus("REJECTED");
        Company saved = companyRepository.save(company);
        auditLogService.log(adminUserId, "REJECT_COMPANY", "COMPANY", companyId, "Từ chối công ty: " + company.getCompanyName());
        return buildCompanyResponse(saved);
    }

    // ==========================================
    // NHÓM 2.2: DUYỆT JOIN REQUEST CHO CÔNG TY CHƯA CÓ COMPANY_ADMIN
    // ==========================================
    public List<CompanyJoinRequestResponse> getPendingJoinRequestsForOrphanCompanies() {
        List<CompanyJoinRequest> pendingRequests = joinRequestRepository.findByStatus("PENDING");

        return pendingRequests.stream()
                .filter(req -> !memberRepository.existsByCompanyIdAndMemberRole(req.getCompany().getId(), "COMPANY_ADMIN"))
                .map(this::buildJoinRequestResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CompanyJoinRequestResponse approveOrphanCompanyJoinRequest(Integer requestId, Integer adminUserId) {
        CompanyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu gia nhập không tồn tại"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý trước đó");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản admin không tồn tại"));

        request.setStatus("APPROVED");
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        CompanyJoinRequest savedReq = joinRequestRepository.save(request);

        // Validation: 1 tài khoản không thể là COMPANY_ADMIN ở 2 công ty khác nhau
        boolean isOtherCompanyAdmin = memberRepository.findByUserId(request.getUser().getId()).stream()
                .anyMatch(m -> !m.getCompany().getId().equals(request.getCompany().getId()) && "COMPANY_ADMIN".equalsIgnoreCase(m.getMemberRole()));
        if (isOtherCompanyAdmin) {
            throw new RuntimeException("Người dùng này đã là Quản trị viên (Company Admin) của một công ty khác. Không thể bổ nhiệm làm Admin cho công ty này.");
        }

        // Set role COMPANY_ADMIN for first member of orphan company
        CompanyMember newMember = new CompanyMember(request.getCompany(), request.getUser(), "COMPANY_ADMIN");
        memberRepository.save(newMember);

        auditLogService.log(adminUserId, "APPROVE_JOIN_REQUEST", "COMPANY_JOIN_REQUEST", requestId,
                "Duyệt Company Admin đầu tiên cho công ty: " + request.getCompany().getCompanyName() + " - User: " + request.getUser().getEmail());

        return buildJoinRequestResponse(savedReq);
    }

    @Transactional
    public CompanyJoinRequestResponse rejectOrphanCompanyJoinRequest(Integer requestId, Integer adminUserId) {
        CompanyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu gia nhập không tồn tại"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý trước đó");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Tài khoản admin không tồn tại"));

        request.setStatus("REJECTED");
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        CompanyJoinRequest savedReq = joinRequestRepository.save(request);

        auditLogService.log(adminUserId, "REJECT_JOIN_REQUEST", "COMPANY_JOIN_REQUEST", requestId,
                "Từ chối yêu cầu gia nhập ID: " + requestId);

        return buildJoinRequestResponse(savedReq);
    }

    // ==========================================
    // NHÓM 2.3: QUẢN LÝ / CAN THIỆP COMPANY_MEMBERS
    // ==========================================
        @Transactional
    public CompanyMemberResponse addCompanyMemberByAdmin(Integer companyId, Integer userId, String role, Integer adminUserId) {
        Company comp = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Công ty không tồn tại"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        String memberRole = role != null && !role.trim().isEmpty() ? role.trim().toUpperCase() : "COMPANY_ADMIN";

        // Validation: 1 tài khoản không thể là COMPANY_ADMIN ở 2 công ty khác nhau
        if ("COMPANY_ADMIN".equalsIgnoreCase(memberRole)) {
            boolean isOtherCompanyAdmin = memberRepository.findByUserId(userId).stream()
                    .anyMatch(m -> !m.getCompany().getId().equals(companyId) && "COMPANY_ADMIN".equalsIgnoreCase(m.getMemberRole()));
            if (isOtherCompanyAdmin) {
                throw new RuntimeException("Tài khoản này đã là Quản trị viên (Company Admin) của một công ty khác. Một tài khoản không thể làm Admin của 2 công ty cùng lúc.");
            }
        }

        if (!"EMPLOYER".equalsIgnoreCase(user.getRole())) {
            user.setRole("EMPLOYER");
            userRepository.save(user);
        }


        CompanyMember member = memberRepository.findByCompanyIdAndUserId(companyId, userId).orElse(null);
        if (member == null) {
            member = new CompanyMember(comp, user, memberRole);
        } else {
            member.setMemberRole(memberRole);
        }
        member.setJoinedAt(LocalDateTime.now());
        CompanyMember saved = memberRepository.save(member);

        auditLogService.log(adminUserId, "ASSIGN_COMPANY_ADMIN", "COMPANY_MEMBER", userId,
                "Gán user " + user.getEmail() + " làm " + memberRole + " cho công ty: " + comp.getCompanyName());

        return buildCompanyMemberResponse(saved);
    }

    public List<CompanyMemberResponse> getCompanyMembers(Integer companyId) {
        List<CompanyMember> members = memberRepository.findByCompanyId(companyId);
        return members.stream().map(this::buildCompanyMemberResponse).collect(Collectors.toList());
    }

    @Transactional
    public CompanyMemberResponse updateCompanyMemberRole(Integer companyId, Integer targetUserId, String newRole, Integer adminUserId) {
        CompanyMember member = memberRepository.findByCompanyIdAndUserId(companyId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Thành viên không thuộc công ty này"));

        String role = newRole != null ? newRole.trim().toUpperCase() : "RECRUITER";
        if (!"COMPANY_ADMIN".equals(role) && !"RECRUITER".equals(role)) {
            throw new RuntimeException("Vai trò không hợp lệ. Chỉ chấp nhận COMPANY_ADMIN hoặc RECRUITER");
        }

        // Validation: 1 tài khoản không thể là COMPANY_ADMIN ở 2 công ty khác nhau
        if ("COMPANY_ADMIN".equalsIgnoreCase(role)) {
            boolean isOtherCompanyAdmin = memberRepository.findByUserId(targetUserId).stream()
                    .anyMatch(m -> !m.getCompany().getId().equals(companyId) && "COMPANY_ADMIN".equalsIgnoreCase(m.getMemberRole()));
            if (isOtherCompanyAdmin) {
                throw new RuntimeException("Tài khoản này đã là Quản trị viên (Company Admin) của một công ty khác. Không thể thăng cấp làm Admin ở công ty này.");
            }
        }

        // Principle: Bảo vệ COMPANY_ADMIN cuối cùng
        if ("COMPANY_ADMIN".equalsIgnoreCase(member.getMemberRole()) && !"COMPANY_ADMIN".equals(role)) {
            long adminCount = memberRepository.countByCompanyIdAndMemberRole(companyId, "COMPANY_ADMIN");
            if (adminCount <= 1) {
                throw new RuntimeException("Không thể hạ quyền Company Admin duy nhất của công ty. Công ty phải có ít nhất 1 người quản trị.");
            }
        }

        member.setMemberRole(role);
        CompanyMember saved = memberRepository.save(member);

        auditLogService.log(adminUserId, "CHANGE_MEMBER_ROLE", "COMPANY_MEMBER", targetUserId,
                "Đổi vai trò thành viên user ID " + targetUserId + " trong công ty ID " + companyId + " thành " + role);

        return buildCompanyMemberResponse(saved);
    }

    @Transactional
    public void removeCompanyMember(Integer companyId, Integer targetUserId, Integer adminUserId) {
        CompanyMember member = memberRepository.findByCompanyIdAndUserId(companyId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Thành viên không thuộc công ty này"));

        // Principle: Bảo vệ COMPANY_ADMIN cuối cùng
        if ("COMPANY_ADMIN".equalsIgnoreCase(member.getMemberRole())) {
            long adminCount = memberRepository.countByCompanyIdAndMemberRole(companyId, "COMPANY_ADMIN");
            if (adminCount <= 1) {
                throw new RuntimeException("Không thể gỡ bỏ Company Admin duy nhất của công ty. Công ty phải có ít nhất 1 người quản trị.");
            }
        }

        memberRepository.delete(member);
        auditLogService.log(adminUserId, "REMOVE_MEMBER", "COMPANY_MEMBER", targetUserId,
                "Gỡ bỏ thành viên user ID " + targetUserId + " khỏi công ty ID " + companyId);
    }

    // ==========================================
    // HELPER BUILDER METHODS
    // ==========================================
    private CompanyResponse buildCompanyResponse(Company comp) {
        List<CompanyMember> members = memberRepository.findByCompanyId(comp.getId());
        Optional<CompanyMember> adminMember = members.stream()
                .filter(m -> "COMPANY_ADMIN".equalsIgnoreCase(m.getMemberRole()))
                .findFirst();

        Integer adminUserId = null;
        String adminEmail = null;
        String adminFullName = null;

        if (adminMember.isPresent()) {
            User u = adminMember.get().getUser();
            adminUserId = u.getId();
            adminEmail = u.getEmail();
            adminFullName = employerProfileRepository.findById(u.getId())
                    .map(EmployerProfile::getFullName).orElse(u.getEmail());
        } else if (comp.getCreatedBy() != null && "EMPLOYER".equalsIgnoreCase(comp.getCreatedBy().getRole())) {
            User u = comp.getCreatedBy();
            adminUserId = u.getId();
            adminEmail = u.getEmail();
            adminFullName = employerProfileRepository.findById(u.getId())
                    .map(EmployerProfile::getFullName).orElse(u.getEmail());
        }

        return CompanyResponse.builder()
                .id(comp.getId())
                .companyName(comp.getCompanyName())
                .taxCode(comp.getTaxCode())
                .phoneNumber(comp.getPhoneNumber())
                .businessLicenseUrl(comp.getBusinessLicenseUrl())
                .companyScale(comp.getCompanyScale())
                .description(comp.getDescription())
                .website(comp.getWebsite())
                .status(comp.getStatus())
                .createdById(comp.getCreatedBy() != null ? comp.getCreatedBy().getId() : null)
                .createdByEmail(comp.getCreatedBy() != null ? comp.getCreatedBy().getEmail() : null)
                .approvedById(comp.getApprovedBy() != null ? comp.getApprovedBy().getId() : null)
                .approvedByEmail(comp.getApprovedBy() != null ? comp.getApprovedBy().getEmail() : null)
                .approvedAt(comp.getApprovedAt())
                .createdAt(comp.getCreatedAt())
                .companyAdminUserId(adminUserId)
                .companyAdminEmail(adminEmail)
                .companyAdminFullName(adminFullName)
                .totalMembers(members.size())
                .build();
    }

    private AdminUserResponse buildAdminUserResponse(User user) {
        String fullName = user.getEmail();
        Integer compId = null;
        String compName = null;
        String compRole = null;

        if ("CANDIDATE".equalsIgnoreCase(user.getRole())) {
            fullName = candidateProfileRepository.findById(user.getId())
                    .map(CandidateProfile::getFullName).orElse(user.getEmail());
        } else if ("EMPLOYER".equalsIgnoreCase(user.getRole())) {
            fullName = employerProfileRepository.findById(user.getId())
                    .map(EmployerProfile::getFullName).orElse(user.getEmail());

            List<CompanyMember> memberships = memberRepository.findByUserId(user.getId());
            if (!memberships.isEmpty()) {
                CompanyMember cm = memberships.get(0);
                compId = cm.getCompany().getId();
                compName = cm.getCompany().getCompanyName();
                compRole = cm.getMemberRole();
            }
        }

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .fullName(fullName)
                .companyId(compId)
                .companyName(compName)
                .companyRole(compRole)
                .build();
    }

    private ReportResponse buildReportResponse(Report r) {
        String resolverName = r.getResolvedBy() != null ? r.getResolvedBy().getEmail() : null;
        String postAuthorEmail = (r.getPost() != null && r.getPost().getAuthor() != null)
                ? r.getPost().getAuthor().getEmail() : null;

        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterEmail(r.getReporter().getEmail())
                .targetType(r.getTargetType())
                .jobId(r.getJob() != null ? r.getJob().getId() : null)
                .jobTitle(r.getJob() != null ? r.getJob().getTitle() : null)
                .postId(r.getPost() != null ? r.getPost().getId() : null)
                .postTitle(r.getPost() != null ? r.getPost().getTitle() : null)
                .postAuthorEmail(postAuthorEmail)
                .reason(r.getReason())
                .status(r.getStatus())
                .resolvedById(r.getResolvedBy() != null ? r.getResolvedBy().getId() : null)
                .resolvedByName(resolverName)
                .resolvedAt(r.getResolvedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private CompanyJoinRequestResponse buildJoinRequestResponse(CompanyJoinRequest req) {
        String reviewedByName = req.getReviewedBy() != null ? req.getReviewedBy().getEmail() : null;
        String userName = req.getUser().getEmail();
        if ("CANDIDATE".equalsIgnoreCase(req.getUser().getRole())) {
            userName = candidateProfileRepository.findById(req.getUser().getId())
                    .map(CandidateProfile::getFullName).orElse(req.getUser().getEmail());
        } else if ("EMPLOYER".equalsIgnoreCase(req.getUser().getRole())) {
            userName = employerProfileRepository.findById(req.getUser().getId())
                    .map(EmployerProfile::getFullName).orElse(req.getUser().getEmail());
        }

        return CompanyJoinRequestResponse.builder()
                .id(req.getId())
                .companyId(req.getCompany().getId())
                .companyName(req.getCompany().getCompanyName())
                .userId(req.getUser().getId())
                .userEmail(req.getUser().getEmail())
                .userName(userName)
                .status(req.getStatus())
                .message(req.getMessage())
                .requestedAt(req.getRequestedAt())
                .reviewedAt(req.getReviewedAt())
                .reviewedById(req.getReviewedBy() != null ? req.getReviewedBy().getId() : null)
                .reviewedByName(reviewedByName)
                .build();
    }

    private CompanyMemberResponse buildCompanyMemberResponse(CompanyMember m) {
        String fullName = m.getUser().getEmail();
        if ("CANDIDATE".equalsIgnoreCase(m.getUser().getRole())) {
            fullName = candidateProfileRepository.findById(m.getUser().getId())
                    .map(CandidateProfile::getFullName).orElse(m.getUser().getEmail());
        } else if ("EMPLOYER".equalsIgnoreCase(m.getUser().getRole())) {
            fullName = employerProfileRepository.findById(m.getUser().getId())
                    .map(EmployerProfile::getFullName).orElse(m.getUser().getEmail());
        }

        return CompanyMemberResponse.builder()
                .id(m.getId())
                .companyId(m.getCompany().getId())
                .companyName(m.getCompany().getCompanyName())
                .userId(m.getUser().getId())
                .email(m.getUser().getEmail())
                .fullName(fullName)
                .memberRole(m.getMemberRole())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}
