package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CompanyJoinRequestDTO;
import com.example.unipathapi.dto.request.CompanyRequest;
import com.example.unipathapi.dto.request.ShiftRequest;
import com.example.unipathapi.dto.response.*;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyManagementService {

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

    @Transactional
    public CompanyResponse createCompany(Integer userId, CompanyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

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

        // Tự động chèn 1 bản ghi vào company_members với member_role = COMPANY_ADMIN cho người tạo
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

        if ("APPROVED".equalsIgnoreCase(company.getStatus())) {
            return buildCompanyResponse(company);
        }

        // Nếu PENDING hoặc REJECTED: chỉ cho xem nếu caller là member hoặc System ADMIN
        boolean isMember = memberRepository.findByCompanyIdAndUserId(companyId, currentUserId).isPresent();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        if (!isMember && !isAdmin) {
            throw new RuntimeException("403: Bạn không có quyền xem thông tin công ty chưa được phê duyệt này");
        }

        return buildCompanyResponse(company);
    }

    @Transactional
    public CompanyResponse updateCompany(Integer companyId, Integer currentUserId, CompanyRequest request) {
        boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, currentUserId, "COMPANY_ADMIN");
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
                .build()).collect(Collectors.toList());
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

    public List<CompanyJoinRequestResponse> getPendingJoinRequestsOfCompany(Integer companyId, Integer currentUserId, String currentUserRole) {
        boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, currentUserId, "COMPANY_ADMIN");
        boolean isSystemAdmin = "ADMIN".equalsIgnoreCase(currentUserRole);

        if (!isCompanyAdmin && !isSystemAdmin) {
            throw new RuntimeException("403: Bạn không có quyền xem danh sách yêu cầu gia nhập của công ty này");
        }

        List<CompanyJoinRequest> requests = joinRequestRepository.findByCompanyIdAndStatus(companyId, "PENDING");
        return requests.stream().map(this::buildJoinRequestResponse).collect(Collectors.toList());
    }

    @Transactional
    public CompanyJoinRequestResponse approveJoinRequest(Integer requestId, Integer reviewerUserId, String reviewerRole) {
        CompanyJoinRequest joinReq = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu gia nhập không tồn tại"));

        Integer companyId = joinReq.getCompany().getId();
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new RuntimeException("Người duyệt không tồn tại"));

        boolean hasCompanyAdmin = memberRepository.existsByCompanyIdAndMemberRole(companyId, "COMPANY_ADMIN");

        if (hasCompanyAdmin) {
            // Nếu đã có COMPANY_ADMIN -> chỉ COMPANY_ADMIN đó mới được duyệt
            boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, reviewerUserId, "COMPANY_ADMIN");
            if (!isCompanyAdmin) {
                throw new RuntimeException("403: Chỉ có COMPANY_ADMIN của công ty mới được phép duyệt yêu cầu gia nhập");
            }
        } else {
            // Nếu chưa có COMPANY_ADMIN -> chỉ System Admin mới được duyệt
            if (!"ADMIN".equalsIgnoreCase(reviewerRole)) {
                throw new RuntimeException("403: Công ty này chưa có COMPANY_ADMIN nào, chỉ có System Admin mới được duyệt yêu cầu");
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
    public CompanyJoinRequestResponse rejectJoinRequest(Integer requestId, Integer reviewerUserId, String reviewerRole) {
        CompanyJoinRequest joinReq = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu gia nhập không tồn tại"));

        Integer companyId = joinReq.getCompany().getId();
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new RuntimeException("Người duyệt không tồn tại"));

        boolean hasCompanyAdmin = memberRepository.existsByCompanyIdAndMemberRole(companyId, "COMPANY_ADMIN");

        if (hasCompanyAdmin) {
            boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, reviewerUserId, "COMPANY_ADMIN");
            if (!isCompanyAdmin) {
                throw new RuntimeException("403: Chỉ có COMPANY_ADMIN của công ty mới được phép từ chối yêu cầu gia nhập");
            }
        } else {
            if (!"ADMIN".equalsIgnoreCase(reviewerRole)) {
                throw new RuntimeException("403: Công ty này chưa có COMPANY_ADMIN nào, chỉ có System Admin mới được từ chối yêu cầu");
            }
        }

        joinReq.setStatus("REJECTED");
        joinReq.setReviewedBy(reviewer);
        joinReq.setReviewedAt(LocalDateTime.now());
        CompanyJoinRequest saved = joinRequestRepository.save(joinReq);

        return buildJoinRequestResponse(saved);
    }

    public List<CompanyMemberResponse> getCompanyMembers(Integer companyId) {
        List<CompanyMember> members = memberRepository.findByCompanyId(companyId);
        return members.stream().map(this::buildCompanyMemberResponse).collect(Collectors.toList());
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

    public List<EmployeeResponse> getEmployees(Integer employerUserId) {
        Company company = getFirstCompanyOfEmployer(employerUserId);
        List<Employment> employments = employmentRepository.findByCompanyId(company.getId());
        return employments.stream().map(this::buildEmployeeResponse).collect(Collectors.toList());
    }

    public List<PayrollResponse> getPayroll(Integer employerUserId, Short month, Short year) {
        Company company = getFirstCompanyOfEmployer(employerUserId);
        List<Payroll> payrolls = payrollRepository.findPayrollByCompanyAndOptionalMonthYear(company.getId(), month, year);
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

    public CompanyResponse buildCompanyResponse(Company comp) {
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
