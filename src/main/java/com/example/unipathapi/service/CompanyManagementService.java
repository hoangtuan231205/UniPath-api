package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CompanyUpdateRequest;
import com.example.unipathapi.dto.request.ShiftRequest;
import com.example.unipathapi.dto.response.CompanyResponse;
import com.example.unipathapi.dto.response.EmployeeResponse;
import com.example.unipathapi.dto.response.PayrollResponse;
import com.example.unipathapi.dto.response.ShiftResponse;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyManagementService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmploymentRepository employmentRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    public CompanyResponse getMyCompany(Integer userId) {
        Company company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Chưa tìm thấy thông tin công ty"));
        return buildCompanyResponse(company);
    }

    public CompanyResponse updateMyCompany(Integer userId, CompanyUpdateRequest request) {
        Company company = companyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
                    Company newComp = new Company();
                    newComp.setUser(user);
                    return newComp;
                });

        company.setCompanyName(request.getCompanyName());
        if (request.getCompanyScale() != null) {
            company.setCompanyScale(request.getCompanyScale());
        }
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());

        Company saved = companyRepository.save(company);
        return buildCompanyResponse(saved);
    }

    // --- SHIFTS ---
    public List<ShiftResponse> getTodayShifts(Integer employerUserId) {
        Company company = getCompanyByEmployer(employerUserId);
        List<Shift> shifts = shiftRepository.findShiftsByCompanyAndDate(company.getId(), LocalDate.now());
        return shifts.stream().map(this::buildShiftResponse).collect(Collectors.toList());
    }

    public ShiftResponse createShift(Integer employerUserId, ShiftRequest request) {
        Company company = getCompanyByEmployer(employerUserId);
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
        Company company = getCompanyByEmployer(employerUserId);
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

    // --- EMPLOYEES ---
    public List<EmployeeResponse> getEmployees(Integer employerUserId) {
        Company company = getCompanyByEmployer(employerUserId);
        List<Employment> employments = employmentRepository.findByCompanyId(company.getId());
        return employments.stream().map(this::buildEmployeeResponse).collect(Collectors.toList());
    }

    // --- PAYROLL ---
    public List<PayrollResponse> getPayroll(Integer employerUserId, Short month, Short year) {
        Company company = getCompanyByEmployer(employerUserId);
        List<Payroll> payrolls = payrollRepository.findPayrollByCompanyAndOptionalMonthYear(company.getId(), month, year);
        return payrolls.stream().map(this::buildPayrollResponse).collect(Collectors.toList());
    }

    // --- HELPER METHODS ---
    private Company getCompanyByEmployer(Integer userId) {
        return companyRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa tạo thông tin công ty"));
    }

    private CompanyResponse buildCompanyResponse(Company comp) {
        return CompanyResponse.builder()
                .id(comp.getId())
                .userId(comp.getUser().getId())
                .companyName(comp.getCompanyName())
                .companyScale(comp.getCompanyScale())
                .description(comp.getDescription())
                .website(comp.getWebsite())
                .build();
    }

    private ShiftResponse buildShiftResponse(Shift shift) {
        String studentName = candidateProfileRepository.findById(shift.getEmployment().getStudent().getId())
                .map(CandidateProfile::getFullName).orElse(shift.getEmployment().getStudent().getEmail());

        return ShiftResponse.builder()
                .id(shift.getId())
                .employmentId(shift.getEmployment().getId())
                .studentId(shift.getEmployment().getStudent().getId())
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
        CandidateProfile candidate = candidateProfileRepository.findById(emp.getStudent().getId()).orElse(null);

        return EmployeeResponse.builder()
                .employmentId(emp.getId())
                .studentId(emp.getStudent().getId())
                .studentName(candidate != null ? candidate.getFullName() : emp.getStudent().getEmail())
                .email(emp.getStudent().getEmail())
                .phone(candidate != null ? candidate.getPhoneNumber() : null)
                .universityName(candidate != null ? candidate.getUniversityName() : null)
                .major(candidate != null ? candidate.getMajor() : null)
                .baseSalaryPerHour(emp.getBaseSalaryPerHour())
                .startDate(emp.getStartDate())
                .status(emp.getStatus())
                .build();
    }

    private PayrollResponse buildPayrollResponse(Payroll pr) {
        String studentName = candidateProfileRepository.findById(pr.getEmployment().getStudent().getId())
                .map(CandidateProfile::getFullName).orElse(pr.getEmployment().getStudent().getEmail());

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
