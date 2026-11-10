package com.example.unipathapi.company.service;
import com.example.unipathapi.company.entity.*;
import com.example.unipathapi.company.repository.*;
import com.example.unipathapi.company.dto.request.*;
import com.example.unipathapi.company.dto.response.*;
import com.example.unipathapi.company.service.*;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.employer.entity.EmployerProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;

import com.example.unipathapi.company.dto.request.CompanyJoinRequestDTO;
import com.example.unipathapi.company.dto.request.CompanyRequest;
import com.example.unipathapi.company.dto.request.ShiftRequest;
import com.example.unipathapi.common.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyManagementService {

    // Helper method to normalize company names (lowercase, remove Vietnamese diacritics, collapse whitespace)
    public static String normalizeCompanyName(String input) {
        if (input == null) return "";
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[đĐ]", "d");
        normalized = normalized.toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    // Trigram-like Jaccard/Dice similarity between two normalized strings
    private static double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.length() < 2 || s2.length() < 2) return 0.0;

        java.util.Set<String> set1 = new java.util.HashSet<>();
        for (int i = 0; i < s1.length() - 1; i++) {
            set1.add(s1.substring(i, i + 2));
        }

        java.util.Set<String> set2 = new java.util.HashSet<>();
        for (int i = 0; i < s2.length() - 1; i++) {
            set2.add(s2.substring(i, i + 2));
        }

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        return (2.0 * intersection.size()) / (set1.size() + set2.size());
    }

    @Transactional
    public com.example.unipathapi.company.dto.response.CompanyProposeResponse proposeCompany(Integer userId, com.example.unipathapi.company.dto.request.CompanyProposeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        String rawName = request.getCompanyName() != null ? request.getCompanyName().trim() : "";
        if (rawName.isEmpty()) {
            throw new RuntimeException("Tên công ty không được để trống");
        }

        String normalizedInput = normalizeCompanyName(rawName);
        List<Company> allCompanies = companyRepository.findAll();

        // 1. Tầng 1: Exact duplicate check
        for (Company c : allCompanies) {
            String normExist = normalizeCompanyName(c.getCompanyName());
            if (normExist.equals(normalizedInput)) {
                throw new RuntimeException("Công ty \"" + c.getCompanyName() + "\" đã tồn tại trong hệ thống. Vui lòng chọn công ty này từ danh sách tìm kiếm thay vì đề xuất mới.");
            }
        }

        // Check taxCode duplicate if provided
        if (request.getTaxCode() != null && !request.getTaxCode().trim().isEmpty()) {
            if (companyRepository.findByTaxCode(request.getTaxCode().trim()).isPresent()) {
                throw new RuntimeException("Mã số thuế đã tồn tại trên hệ thống");
            }
        }

        // 2. Tầng 2: Fuzzy matching warning (only if not confirmed and length >= 4)
        if (!Boolean.TRUE.equals(request.getConfirmed()) && normalizedInput.length() >= 4) {
            List<com.example.unipathapi.company.dto.response.CompanyProposeResponse.SimilarCompanyDTO> similarList = new java.util.ArrayList<>();
            for (Company c : allCompanies) {
                String normExist = normalizeCompanyName(c.getCompanyName());
                double sim = calculateSimilarity(normalizedInput, normExist);
                if (sim >= 0.65) {
                    similarList.add(com.example.unipathapi.company.dto.response.CompanyProposeResponse.SimilarCompanyDTO.builder()
                            .id(c.getId())
                            .companyName(c.getCompanyName())
                            .similarity(Math.round(sim * 100.0) / 100.0)
                            .build());
                }
            }

            if (!similarList.isEmpty()) {
                similarList.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
                return com.example.unipathapi.company.dto.response.CompanyProposeResponse.builder()
                        .success(false)
                        .duplicate(false)
                        .warning(true)
                        .message("Có vẻ tên công ty bạn nhập tương tự với một công ty đã tồn tại trong hệ thống.")
                        .similarCompanies(similarList)
                        .build();
            }
        }

        // 3. Insert Company with status PENDING
        Company newCompany = new Company();
        newCompany.setCompanyName(rawName);
        newCompany.setTaxCode(request.getTaxCode() != null ? request.getTaxCode().trim() : null);
        newCompany.setCompanyScale(request.getCompanyScale() != null ? request.getCompanyScale() : "SME");
        newCompany.setPhoneNumber(request.getPhoneNumber());
        newCompany.setWebsite(request.getWebsite());
        newCompany.setDescription(request.getDescription());
        newCompany.setStatus("PENDING");
        newCompany.setCreatedBy(user);

        Company saved = companyRepository.save(newCompany);

        // Tự động thêm 1 dòng company_members cho người đề xuất với vai trò COMPANY_ADMIN
        CompanyMember creatorMember = new CompanyMember(saved, user, "COMPANY_ADMIN");
        memberRepository.save(creatorMember);

        return com.example.unipathapi.company.dto.response.CompanyProposeResponse.builder()
                .success(true)
                .duplicate(false)
                .warning(false)
                .companyId(saved.getId())
                .companyName(saved.getCompanyName())
                .message("Đề xuất công ty thành công. Vui lòng chờ Quản trị viên (SUPERADMIN) phê duyệt.")
                .build();
    }


    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository memberRepository;

    @Autowired
    private CompanyJoinRequestRepository joinRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private EmploymentRepository employmentRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private com.example.unipathapi.job.repository.JobRepository jobRepository;

    @Autowired
    private com.example.unipathapi.application.repository.ApplicationRepository applicationRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Transactional
    public CompanyResponse createCompany(Integer userId, CompanyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Validation: 1 tài khoản không thể là COMPANY_ADMIN ở 2 công ty khác nhau
        boolean alreadyAdmin = memberRepository.findByUserId(userId).stream()
                .anyMatch(m -> "COMPANY_ADMIN".equalsIgnoreCase(m.getMemberRole()));
        if (alreadyAdmin) {
            throw new RuntimeException("Tài khoản này đã là Quản trị viên (Company Admin) của một công ty khác. Một tài khoản không thể làm Admin của 2 công ty cùng lúc.");
        }

        if (request.getTaxCode() != null && !request.getTaxCode().trim().isEmpty()) {
            if (companyRepository.findByTaxCode(request.getTaxCode()).isPresent()) {
                throw new RuntimeException("Mã số thuế đã tồn tại trên hệ thống");
            }
        }

        Company company = new Company();
        company.setCompanyName(request.getCompanyName());
        company.setTaxCode(request.getTaxCode());
        company.setPhoneNumber(request.getPhoneNumber());
        company.setBusinessLicenseUrl(request.getBusinessLicenseUrl());
        company.setCompanyScale(request.getCompanyScale() != null ? request.getCompanyScale() : "SME");
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setStatus("PENDING"); // Đợi System Admin duyệt
        company.setCreatedBy(user);

        Company savedCompany = companyRepository.save(company);

        // Tự động chèn 1 bản ghi vào company_members với member_role = COMPANY_ADMIN
        // cho người tạo
        CompanyMember creatorMember = new CompanyMember(savedCompany, user, "COMPANY_ADMIN");
        memberRepository.save(creatorMember);

        return buildCompanyResponse(savedCompany);
    }

    public List<CompanyResponse> searchCompanies(String keyword) {
        String query = keyword != null ? keyword.trim() : "";
        List<Company> companies = companyRepository.findByCompanyNameContainingIgnoreCaseAndStatus(query, "APPROVED");
        return companies.stream().map(this::buildCompanyResponse).collect(Collectors.toList());
    }

    public CompanyResponse getCompanyDetail(Integer companyId, Integer currentUserId, String currentUserRole) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty"));

        boolean isCompanyAdmin = currentUserId != null &&
                memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, currentUserId, "COMPANY_ADMIN");
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        if (!"APPROVED".equalsIgnoreCase(company.getStatus())) {
            // Nếu PENDING hoặc REJECTED: chỉ cho xem nếu caller là member hoặc System ADMIN
            boolean isMember = currentUserId != null && memberRepository.findByCompanyIdAndUserId(companyId, currentUserId).isPresent();
            if (!isMember && !isAdmin) {
                throw new RuntimeException("403: Bạn không có quyền xem thông tin công ty chưa được phê duyệt này");
            }
        }

        return buildCompanyResponse(company, isCompanyAdmin || isAdmin);
    }

    @Transactional
    public CompanyResponse updateCompany(Integer companyId, Integer currentUserId, CompanyRequest request) {
        boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, currentUserId,
                "COMPANY_ADMIN");
        if (!isCompanyAdmin) {
            throw new RuntimeException("403: Chỉ có COMPANY_ADMIN của công ty mới có quyền chỉnh sửa hồ sơ");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty"));

        company.setCompanyName(request.getCompanyName());
        if (request.getCompanyScale() != null) {
            company.setCompanyScale(request.getCompanyScale());
        }
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setTaxCode(request.getTaxCode());
        company.setPhoneNumber(request.getPhoneNumber());
        company.setBusinessLicenseUrl(request.getBusinessLicenseUrl());

        Company saved = companyRepository.save(company);
        return buildCompanyResponse(saved);
    }

    @Transactional
    public CompanyResponse uploadCompanyLogo(Integer companyId, Integer currentUserId, String currentUserRole, MultipartFile file) {
        boolean isCompanyAdmin = currentUserId != null &&
                memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, currentUserId, "COMPANY_ADMIN");
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        if (!isCompanyAdmin && !isAdmin) {
            throw new RuntimeException("403: Chỉ Quản trị viên công ty (COMPANY_ADMIN) hoặc Quản trị viên hệ thống mới có quyền đổi logo công ty");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty"));

        String oldLogo = company.getLogoUrl();
        String newLogoUrl = fileStorageService.storeAvatar(file, "companies/" + companyId);
        company.setLogoUrl(newLogoUrl);
        Company saved = companyRepository.save(company);

        if (oldLogo != null && oldLogo.startsWith("/uploads/companies/")) {
            fileStorageService.deleteFile(oldLogo);
        }

        return buildCompanyResponse(saved, isCompanyAdmin || isAdmin);
    }

    @Transactional
    public CompanyResponse updateCompanyHrmStatus(Integer companyId, Integer currentUserId, Boolean hrmEnabled) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("400: Không tìm thấy công ty với ID: " + companyId));

        boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(
                companyId, currentUserId, "COMPANY_ADMIN"
        );
        if (!isCompanyAdmin) {
            throw new RuntimeException("403: Chỉ Quản trị viên công ty (COMPANY_ADMIN) mới có quyền bật hoặc tắt tính năng Quản lý nhân sự (HRM)");
        }

        company.setHrmEnabled(Boolean.TRUE.equals(hrmEnabled));
        Company saved = companyRepository.save(company);
        return buildCompanyResponse(saved, true);
    }

    public List<UserCompanyMembershipResponse> getUserCompanies(Integer userId) {
        List<CompanyMember> memberships = memberRepository.findByUserId(userId);
        return memberships.stream().map(m -> UserCompanyMembershipResponse.builder()
                .companyId(m.getCompany().getId())
                .companyName(m.getCompany().getCompanyName())
                .taxCode(m.getCompany().getTaxCode())
                .companyScale(m.getCompany().getCompanyScale())
                .description(m.getCompany().getDescription())
                .website(m.getCompany().getWebsite())
                .status(m.getCompany().getStatus())
                .memberRole(m.getMemberRole())
                .joinedAt(m.getJoinedAt())
                .hrmEnabled(Boolean.TRUE.equals(m.getCompany().getHrmEnabled()))
                .build()).collect(Collectors.toList());
    }

    public MyCompanyOverviewResponse getMyCompanyOverview(Integer userId, Integer requestedCompanyId) {
        List<CompanyMember> memberships = memberRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return MyCompanyOverviewResponse.builder()
                    .hasCompany(false)
                    .build();
        }

        // Quy tắc đa công ty: Có nhiều hơn 1 membership và không truyền companyId
        if (memberships.size() > 1 && requestedCompanyId == null) {
            List<UserCompanyMembershipResponse> available = memberships.stream()
                    .map(m -> UserCompanyMembershipResponse.builder()
                            .companyId(m.getCompany().getId())
                            .companyName(m.getCompany().getCompanyName())
                            .companyScale(m.getCompany().getCompanyScale())
                            .description(m.getCompany().getDescription())
                            .website(m.getCompany().getWebsite())
                            .status(m.getCompany().getStatus())
                            .memberRole(m.getMemberRole())
                            .joinedAt(m.getJoinedAt())
                            .hrmEnabled(Boolean.TRUE.equals(m.getCompany().getHrmEnabled()))
                            .build())
                    .collect(Collectors.toList());

            return MyCompanyOverviewResponse.builder()
                    .hasCompany(true)
                    .requiresCompanySelection(true)
                    .availableCompanies(available)
                    .build();
        }

        CompanyMember targetMember;
        if (requestedCompanyId != null) {
            targetMember = memberRepository.findByCompanyIdAndUserId(requestedCompanyId, userId)
                    .orElseThrow(() -> new RuntimeException("403: Bạn không phải là thành viên của công ty này"));
        } else {
            targetMember = memberships.get(0);
        }

        Company company = targetMember.getCompany();
        Integer companyId = company.getId();
        String memberRole = targetMember.getMemberRole();

        long totalJobs = jobRepository.countByCompanyIdIn(java.util.Collections.singletonList(companyId));
        long activeJobs = jobRepository.countActiveByCompanyIds(java.util.Collections.singletonList(companyId));
        long totalApplications = applicationRepository.countByCompanyIds(java.util.Collections.singletonList(companyId));
        long totalMembers = memberRepository.countByCompanyId(companyId);
        long totalLocations = locationRepository.countByCompanyId(companyId);

        boolean isCompanyAdmin = "COMPANY_ADMIN".equalsIgnoreCase(memberRole);

        return MyCompanyOverviewResponse.builder()
                .hasCompany(true)
                .requiresCompanySelection(false)
                .companyId(companyId)
                .companyName(company.getCompanyName())
                .companyScale(company.getCompanyScale())
                .description(company.getDescription())
                .website(company.getWebsite())
                .phoneNumber(company.getPhoneNumber())
                .logoUrl(company.getLogoUrl())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .currentUserMemberRole(memberRole)
                .taxCode(isCompanyAdmin ? company.getTaxCode() : null)
                .businessLicenseUrl(isCompanyAdmin ? company.getBusinessLicenseUrl() : null)
                .approvedAt(isCompanyAdmin ? company.getApprovedAt() : null)
                .approvedByEmail(isCompanyAdmin && company.getApprovedBy() != null ? company.getApprovedBy().getEmail() : null)
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .totalApplications(totalApplications)
                .totalMembers(totalMembers)
                .totalLocations(totalLocations)
                .hrmEnabled(Boolean.TRUE.equals(company.getHrmEnabled()))
                .build();
    }

    // --- COMPANY JOIN REQUESTS ---
    @Transactional
    public CompanyJoinRequestResponse requestJoinCompany(Integer companyId, Integer userId, CompanyJoinRequestDTO dto) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty"));

        if (!"APPROVED".equalsIgnoreCase(company.getStatus())) {
            throw new RuntimeException("Công ty này chưa được System Admin duyệt");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (memberRepository.findByCompanyIdAndUserId(companyId, userId).isPresent()) {
            throw new RuntimeException("Bạn đã là thành viên của công ty này");
        }

        if (joinRequestRepository.existsByUserIdAndCompanyIdAndStatus(userId, companyId, "PENDING")) {
            throw new RuntimeException("Bạn đã gửi yêu cầu gia nhập công ty này và đang chờ duyệt");
        }

        CompanyJoinRequest joinReq = new CompanyJoinRequest();
        joinReq.setUser(user);
        joinReq.setCompany(company);
        joinReq.setMessage(dto.getMessage());
        joinReq.setStatus("PENDING");

        try {
            CompanyJoinRequest saved = joinRequestRepository.save(joinReq);
            return buildJoinRequestResponse(saved);
        } catch (Exception e) {
            throw new RuntimeException("Bạn đã gửi yêu cầu gia nhập công ty này và đang chờ duyệt");
        }
    }

    public List<CompanyJoinRequestResponse> getPendingJoinRequestsOfCompany(Integer companyId, Integer currentUserId,
            String currentUserRole) {
        boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, currentUserId,
                "COMPANY_ADMIN");
        boolean isSystemAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        if (!isCompanyAdmin && !isSystemAdmin) {
            throw new RuntimeException("403: Bạn không có quyền xem danh sách yêu cầu gia nhập của công ty này");
        }

        List<CompanyJoinRequest> requests = joinRequestRepository.findByCompanyIdAndStatus(companyId, "PENDING");
        return requests.stream().map(this::buildJoinRequestResponse).collect(Collectors.toList());
    }

    @Transactional
    public CompanyJoinRequestResponse approveJoinRequest(Integer requestId, Integer reviewerUserId,
            String reviewerRole) {
        CompanyJoinRequest joinReq = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu gia nhập không tồn tại"));

        Integer companyId = joinReq.getCompany().getId();
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new RuntimeException("Người duyệt không tồn tại"));

        boolean hasCompanyAdmin = memberRepository.existsByCompanyIdAndMemberRole(companyId, "COMPANY_ADMIN");

        if (hasCompanyAdmin) {
            // Nếu đã có COMPANY_ADMIN -> chỉ COMPANY_ADMIN đó mới được duyệt
            boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, reviewerUserId,
                    "COMPANY_ADMIN");
            if (!isCompanyAdmin) {
                throw new RuntimeException(
                        "403: Chỉ có COMPANY_ADMIN của công ty mới được phép duyệt yêu cầu gia nhập");
            }
        } else {
            // Nếu chưa có COMPANY_ADMIN -> chỉ System Admin mới được duyệt
            if (!"ADMIN".equalsIgnoreCase(reviewerRole)) {
                throw new RuntimeException(
                        "403: Công ty này chưa có COMPANY_ADMIN nào, chỉ có System Admin mới được duyệt yêu cầu");
            }
        }

        joinReq.setStatus("APPROVED");
        joinReq.setReviewedBy(reviewer);
        joinReq.setReviewedAt(LocalDateTime.now());
        CompanyJoinRequest saved = joinRequestRepository.save(joinReq);

        // Chèn vào company_members với member_role = RECRUITER
        CompanyMember newMember = new CompanyMember(joinReq.getCompany(), joinReq.getUser(), "RECRUITER");
        memberRepository.save(newMember);

        return buildJoinRequestResponse(saved);
    }

    @Transactional
    public CompanyJoinRequestResponse rejectJoinRequest(Integer requestId, Integer reviewerUserId,
            String reviewerRole) {
        CompanyJoinRequest joinReq = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu gia nhập không tồn tại"));

        Integer companyId = joinReq.getCompany().getId();
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new RuntimeException("Người duyệt không tồn tại"));

        boolean hasCompanyAdmin = memberRepository.existsByCompanyIdAndMemberRole(companyId, "COMPANY_ADMIN");

        if (hasCompanyAdmin) {
            boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, reviewerUserId,
                    "COMPANY_ADMIN");
            if (!isCompanyAdmin) {
                throw new RuntimeException(
                        "403: Chỉ có COMPANY_ADMIN của công ty mới được phép từ chối yêu cầu gia nhập");
            }
        } else {
            if (!"ADMIN".equalsIgnoreCase(reviewerRole)) {
                throw new RuntimeException(
                        "403: Công ty này chưa có COMPANY_ADMIN nào, chỉ có System Admin mới được từ chối yêu cầu");
            }
        }

        joinReq.setStatus("REJECTED");
        joinReq.setReviewedBy(reviewer);
        joinReq.setReviewedAt(LocalDateTime.now());
        CompanyJoinRequest saved = joinRequestRepository.save(joinReq);

        return buildJoinRequestResponse(saved);
    }

    public List<CompanyMemberResponse> getCompanyMembers(Integer companyId, Integer currentUserId, String currentUserRole) {
        if (currentUserId == null) {
            throw new RuntimeException("403: Chưa xác thực hoặc thiếu token");
        }

        CompanyMember callerMembership = memberRepository.findByCompanyIdAndUserId(companyId, currentUserId).orElse(null);
        boolean isSystemAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        if (callerMembership == null && !isSystemAdmin) {
            throw new RuntimeException("403: Bạn không phải thành viên của công ty này");
        }

        boolean isCompanyAdmin = isSystemAdmin || (callerMembership != null && "COMPANY_ADMIN".equalsIgnoreCase(callerMembership.getMemberRole()));

        List<CompanyMember> members = memberRepository.findByCompanyId(companyId);
        return members.stream().map(m -> {
            String fullName = employerProfileRepository.findById(m.getUser().getId())
                    .map(EmployerProfile::getFullName).orElse(m.getUser().getEmail());
            String email = m.getUser().getEmail();
            if (!isCompanyAdmin && !m.getUser().getId().equals(currentUserId)) {
                email = maskEmail(email);
            }

            return CompanyMemberResponse.builder()
                    .id(m.getId())
                    .companyId(m.getCompany().getId())
                    .companyName(m.getCompany().getCompanyName())
                    .userId(m.getUser().getId())
                    .fullName(fullName)
                    .email(email)
                    .memberRole(m.getMemberRole())
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    // --- SHIFTS & EMPLOYEES & PAYROLL ---
    public List<ShiftResponse> getTodayShifts(Integer employerUserId) {
        Company company = getFirstCompanyOfEmployer(employerUserId);
        List<Shift> shifts = shiftRepository.findShiftsByCompanyAndDate(company.getId(), LocalDate.now());
        return shifts.stream().map(this::buildShiftResponse).collect(Collectors.toList());
    }

    public ShiftResponse createShift(Integer employerUserId, ShiftRequest request) {
        Company company = getFirstCompanyOfEmployer(employerUserId);
        Employment employment = employmentRepository.findById(request.getEmploymentId())
                .orElseThrow(() -> new RuntimeException("Hợp đồng nhân sự không tồn tại"));

        if (!employment.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("Nhân viên này không thuộc công ty của bạn");
        }

        Shift shift = new Shift();
        shift.setEmployment(employment);
        shift.setShiftDate(request.getShiftDate());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        if (request.getStatus() != null) {
            shift.setStatus(request.getStatus());
        }
        if (request.getLocationId() != null) {
            CompanyLocation location = locationRepository.findById(request.getLocationId()).orElse(null);
            shift.setLocation(location);
        }

        Shift saved = shiftRepository.save(shift);
        return buildShiftResponse(saved);
    }

    public ShiftResponse updateShift(Integer shiftId, Integer employerUserId, ShiftRequest request) {
        Company company = getFirstCompanyOfEmployer(employerUserId);
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Ca làm không tồn tại"));

        if (!shift.getEmployment().getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa ca làm này");
        }

        shift.setShiftDate(request.getShiftDate());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        if (request.getStatus() != null) {
            shift.setStatus(request.getStatus());
        }
        if (request.getLocationId() != null) {
            CompanyLocation location = locationRepository.findById(request.getLocationId()).orElse(null);
            shift.setLocation(location);
        }

        Shift updated = shiftRepository.save(shift);
        return buildShiftResponse(updated);
    }

    public EmployeeResponse createEmployment(Integer employerUserId, com.example.unipathapi.company.dto.request.EmploymentRequest request) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Không thể tạo nhân viên trực tiếp ở trạng thái ACTIVE. Vui lòng sử dụng luồng gửi lời mời nhân sự (POST /api/employer/companies/{companyId}/invitations) và đợi ứng viên xác nhận đồng ý."
        );
    }

    public List<EmployeeResponse> getEmployees(Integer employerUserId) {
        Company company = getFirstCompanyOfEmployer(employerUserId);
        List<Employment> employments = employmentRepository.findByCompanyId(company.getId());
        return employments.stream().map(this::buildEmployeeResponse).collect(Collectors.toList());
    }

    public List<PayrollResponse> getPayroll(Integer employerUserId, Short month, Short year) {
        Company company = getFirstCompanyOfEmployer(employerUserId);
        List<Payroll> payrolls = payrollRepository.findPayrollByCompanyAndOptionalMonthYear(company.getId(), month,
                year);
        return payrolls.stream().map(this::buildPayrollResponse).collect(Collectors.toList());
    }

    // --- HELPER METHODS ---
    private Company getFirstCompanyOfEmployer(Integer userId) {
        List<CompanyMember> members = memberRepository.findByUserId(userId);
        if (members.isEmpty()) {
            throw new RuntimeException("Bạn chưa thuộc công ty nào trên hệ thống");
        }
        return members.get(0).getCompany();
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIdx = email.indexOf('@');
        String name = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (name.length() <= 1) {
            return name + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    public CompanyResponse buildCompanyResponse(Company comp) {
        return buildCompanyResponse(comp, false);
    }

    public CompanyResponse buildCompanyResponse(Company comp, boolean includeSensitive) {
        return CompanyResponse.builder()
                .id(comp.getId())
                .companyName(comp.getCompanyName())
                .companyScale(comp.getCompanyScale())
                .description(comp.getDescription())
                .website(comp.getWebsite())
                .phoneNumber(comp.getPhoneNumber())
                .logoUrl(comp.getLogoUrl())
                .status(comp.getStatus())
                .createdAt(comp.getCreatedAt())
                .taxCode(includeSensitive ? comp.getTaxCode() : null)
                .businessLicenseUrl(includeSensitive ? comp.getBusinessLicenseUrl() : null)
                .createdById(includeSensitive && comp.getCreatedBy() != null ? comp.getCreatedBy().getId() : null)
                .createdByEmail(includeSensitive && comp.getCreatedBy() != null ? comp.getCreatedBy().getEmail() : null)
                .approvedById(includeSensitive && comp.getApprovedBy() != null ? comp.getApprovedBy().getId() : null)
                .approvedByEmail(includeSensitive && comp.getApprovedBy() != null ? comp.getApprovedBy().getEmail() : null)
                .approvedAt(includeSensitive ? comp.getApprovedAt() : null)
                .hrmEnabled(Boolean.TRUE.equals(comp.getHrmEnabled()))
                .build();
    }

    private CompanyJoinRequestResponse buildJoinRequestResponse(CompanyJoinRequest req) {
        String userName = employerProfileRepository.findById(req.getUser().getId())
                .map(EmployerProfile::getFullName).orElse(req.getUser().getEmail());

        return CompanyJoinRequestResponse.builder()
                .id(req.getId())
                .userId(req.getUser().getId())
                .userName(userName)
                .userEmail(req.getUser().getEmail())
                .companyId(req.getCompany().getId())
                .companyName(req.getCompany().getCompanyName())
                .status(req.getStatus())
                .message(req.getMessage())
                .requestedAt(req.getRequestedAt())
                .reviewedAt(req.getReviewedAt())
                .reviewedByName(req.getReviewedBy() != null ? req.getReviewedBy().getEmail() : null)
                .build();
    }

    private CompanyMemberResponse buildCompanyMemberResponse(CompanyMember m) {
        String fullName = employerProfileRepository.findById(m.getUser().getId())
                .map(EmployerProfile::getFullName).orElse(m.getUser().getEmail());

        return CompanyMemberResponse.builder()
                .id(m.getId())
                .companyId(m.getCompany().getId())
                .companyName(m.getCompany().getCompanyName())
                .userId(m.getUser().getId())
                .fullName(fullName)
                .email(m.getUser().getEmail())
                .memberRole(m.getMemberRole())
                .joinedAt(m.getJoinedAt())
                .build();
    }

    private ShiftResponse buildShiftResponse(Shift shift) {
        String studentName = candidateProfileRepository.findById(shift.getEmployment().getCandidate().getId())
                .map(CandidateProfile::getFullName).orElse(shift.getEmployment().getCandidate().getEmail());

        return ShiftResponse.builder()
                .id(shift.getId())
                .employmentId(shift.getEmployment().getId())
                .studentId(shift.getEmployment().getCandidate().getId())
                .studentName(studentName)
                .shiftDate(shift.getShiftDate())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .status(shift.getStatus())
                .locationId(shift.getLocation() != null ? shift.getLocation().getId() : null)
                .locationAddress(shift.getLocation() != null ? shift.getLocation().getAddress() : null)
                .build();
    }

    private EmployeeResponse buildEmployeeResponse(Employment emp) {
        CandidateProfile candidate = candidateProfileRepository.findById(emp.getCandidate().getId()).orElse(null);

        return EmployeeResponse.builder()
                .employmentId(emp.getId())
                .studentId(emp.getCandidate().getId())
                .studentName(candidate != null ? candidate.getFullName() : emp.getCandidate().getEmail())
                .email(emp.getCandidate().getEmail())
                .phone(candidate != null ? candidate.getPhoneNumber() : null)
                .universityName(candidate != null ? candidate.getUniversityName() : null)
                .major(candidate != null ? candidate.getMajor() : null)
                .baseSalaryPerHour(emp.getBaseSalaryPerHour())
                .baseSalaryMonthly(emp.getBaseSalaryMonthly())
                .salaryType(emp.getSalaryType())
                .position(emp.getPosition())
                .invitationId(emp.getInvitation() != null ? emp.getInvitation().getId() : null)
                .companyId(emp.getCompany() != null ? emp.getCompany().getId() : null)
                .companyName(emp.getCompany() != null ? emp.getCompany().getCompanyName() : null)
                .startDate(emp.getStartDate())
                .status(emp.getStatus())
                .build();
    }

    private PayrollResponse buildPayrollResponse(Payroll pr) {
        String studentName = candidateProfileRepository.findById(pr.getEmployment().getCandidate().getId())
                .map(CandidateProfile::getFullName).orElse(pr.getEmployment().getCandidate().getEmail());

        return PayrollResponse.builder()
                .id(pr.getId())
                .employmentId(pr.getEmployment().getId())
                .employeeName(studentName)
                .monthYear(pr.getMonthYear())
                .payMonth(pr.getPayMonth())
                .payYear(pr.getPayYear())
                .totalHours(pr.getTotalHours())
                .totalPenalties(pr.getTotalPenalties())
                .finalSalary(pr.getFinalSalary())
                .status(pr.getStatus())
                .build();
    }
}
