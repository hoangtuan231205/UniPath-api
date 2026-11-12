package com.example.unipathapi.company.unit;

import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.company.controller.CompanyController;
import com.example.unipathapi.company.controller.CompanyLocationController;
import com.example.unipathapi.company.dto.request.CompanyRequest;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyLocation;
import com.example.unipathapi.company.entity.CompanyMember;
import com.example.unipathapi.company.repository.CompanyLocationRepository;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.service.CompanyLocationService;
import com.example.unipathapi.company.service.CompanyManagementService;
import com.example.unipathapi.employer.entity.EmployerProfile;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.job.repository.JobRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class EmployerCompanyManagementTest {

    private MockMvc mockMvcCompany;
    private MockMvc mockMvcLocation;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private CompanyLocationRepository locationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployerProfileRepository employerProfileRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private CompanyManagementService companyService;

    @InjectMocks
    private CompanyLocationService locationService;

    private CompanyController companyController;
    private CompanyLocationController locationController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    private User userAdmin;
    private User userRecruiter;
    private Company companyA;
    private Company companyB;
    private CompanyMember memberAdminA;
    private CompanyMember memberRecruiterA;
    private CompanyMember memberRecruiterB;

    @BeforeEach
    void setUp() {
        companyController = new CompanyController();
        ReflectionTestUtils.setField(companyController, "companyService", companyService);
        ReflectionTestUtils.setField(companyController, "securityUtil", securityUtil);

        locationController = new CompanyLocationController();
        ReflectionTestUtils.setField(locationController, "companyLocationService", locationService);
        ReflectionTestUtils.setField(locationController, "securityUtil", securityUtil);

        mockMvcCompany = MockMvcBuilders.standaloneSetup(companyController).build();
        mockMvcLocation = MockMvcBuilders.standaloneSetup(locationController).build();

        // Seed common entities
        userAdmin = new User();
        userAdmin.setId(10);
        userAdmin.setEmail("admin@company.com");

        userRecruiter = new User();
        userRecruiter.setId(20);
        userRecruiter.setEmail("recruiter@company.com");

        companyA = new Company();
        companyA.setId(1);
        companyA.setCompanyName("FPT Software");
        companyA.setCompanyScale("ENTERPRISE");
        companyA.setTaxCode("0101248141");
        companyA.setBusinessLicenseUrl("https://example.com/license-fpt.pdf");
        companyA.setStatus("APPROVED");
        companyA.setCreatedAt(LocalDateTime.now().minusDays(100));

        companyB = new Company();
        companyB.setId(2);
        companyB.setCompanyName("VNG Corporation");
        companyB.setCompanyScale("ENTERPRISE");
        companyB.setTaxCode("0303889911");
        companyB.setStatus("APPROVED");

        memberAdminA = new CompanyMember(companyA, userAdmin, "COMPANY_ADMIN");
        memberAdminA.setId(101);

        memberRecruiterA = new CompanyMember(companyA, userRecruiter, "RECRUITER");
        memberRecruiterA.setId(102);

        memberRecruiterB = new CompanyMember(companyB, userRecruiter, "RECRUITER");
        memberRecruiterB.setId(103);
    }

    // 1. GET /api/companies/me - User chưa có công ty
    @Test
    @DisplayName("GET /api/companies/me - User chưa có công ty trả hasCompany: false")
    void test_GetCompanyMe_UserWithoutCompany_ReturnsEmptyState() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(99);
        when(memberRepository.findByUserId(99)).thenReturn(Collections.emptyList());

        mockMvcCompany.perform(get("/api/companies/me")
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCompany").value(false))
                .andExpect(jsonPath("$.companyName").doesNotExist());
    }

    // 2. GET /api/companies/me - User có đúng 1 công ty và không truyền companyId
    @Test
    @DisplayName("GET /api/companies/me - User có đúng 1 công ty tự động trả chi tiết và role COMPANY_ADMIN kèm số liệu thống kê")
    void test_GetCompanyMe_UserWithOneCompany_ReturnsOverview() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);
        when(memberRepository.findByUserId(10)).thenReturn(List.of(memberAdminA));

        when(jobRepository.countByCompanyIdIn(any())).thenReturn(15L);
        when(jobRepository.countActiveByCompanyIds(any())).thenReturn(8L);
        when(applicationRepository.countByCompanyIds(any())).thenReturn(42L);
        when(memberRepository.countByCompanyId(1)).thenReturn(5L);
        when(locationRepository.countByCompanyId(1)).thenReturn(2L);

        mockMvcCompany.perform(get("/api/companies/me")
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCompany").value(true))
                .andExpect(jsonPath("$.requiresCompanySelection").value(false))
                .andExpect(jsonPath("$.companyId").value(1))
                .andExpect(jsonPath("$.companyName").value("FPT Software"))
                .andExpect(jsonPath("$.currentUserMemberRole").value("COMPANY_ADMIN"))
                .andExpect(jsonPath("$.taxCode").value("0101248141"))
                .andExpect(jsonPath("$.businessLicenseUrl").value("https://example.com/license-fpt.pdf"))
                .andExpect(jsonPath("$.totalJobs").value(15))
                .andExpect(jsonPath("$.activeJobs").value(8))
                .andExpect(jsonPath("$.totalApplications").value(42))
                .andExpect(jsonPath("$.totalMembers").value(5))
                .andExpect(jsonPath("$.totalLocations").value(2));
    }

    // 3. GET /api/companies/me - User có nhiều membership nhưng không truyền companyId
    @Test
    @DisplayName("GET /api/companies/me - User có nhiều membership không truyền companyId phải trả requiresCompanySelection = true và không lộ chi tiết công ty")
    void test_GetCompanyMe_UserWithMultipleCompanies_NoParam_RequiresSelection() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);
        when(memberRepository.findByUserId(20)).thenReturn(List.of(memberRecruiterA, memberRecruiterB));

        mockMvcCompany.perform(get("/api/companies/me")
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCompany").value(true))
                .andExpect(jsonPath("$.requiresCompanySelection").value(true))
                .andExpect(jsonPath("$.availableCompanies", hasSize(2)))
                .andExpect(jsonPath("$.availableCompanies[0].companyId").value(1))
                .andExpect(jsonPath("$.availableCompanies[1].companyId").value(2))
                .andExpect(jsonPath("$.companyName").doesNotExist())
                .andExpect(jsonPath("$.totalJobs").doesNotExist());
    }

    // 4. GET /api/companies/me?companyId=2 - User có nhiều membership chọn companyId hợp lệ
    @Test
    @DisplayName("GET /api/companies/me?companyId=2 - Trả đúng công ty được chọn khi người dùng xác thực hợp lệ")
    void test_GetCompanyMe_WithSelectedCompanyId_ReturnsSelectedCompany() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);
        when(memberRepository.findByUserId(20)).thenReturn(List.of(memberRecruiterA, memberRecruiterB));
        when(memberRepository.findByCompanyIdAndUserId(2, 20)).thenReturn(Optional.of(memberRecruiterB));

        when(jobRepository.countByCompanyIdIn(any())).thenReturn(4L);
        when(jobRepository.countActiveByCompanyIds(any())).thenReturn(2L);
        when(applicationRepository.countByCompanyIds(any())).thenReturn(10L);
        when(memberRepository.countByCompanyId(2)).thenReturn(3L);
        when(locationRepository.countByCompanyId(2)).thenReturn(1L);

        mockMvcCompany.perform(get("/api/companies/me?companyId=2")
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasCompany").value(true))
                .andExpect(jsonPath("$.requiresCompanySelection").value(false))
                .andExpect(jsonPath("$.companyId").value(2))
                .andExpect(jsonPath("$.companyName").value("VNG Corporation"))
                .andExpect(jsonPath("$.currentUserMemberRole").value("RECRUITER"));
    }

    // 5. GET /api/companies/me?companyId=999 - Tampering companyId không thuộc quyền
    @Test
    @DisplayName("GET /api/companies/me?companyId=999 - Trả về 403 Forbidden khi truyền companyId không thuộc quyền")
    void test_GetCompanyMe_TamperingCompanyId_ThrowsForbidden() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);
        when(memberRepository.findByUserId(20)).thenReturn(List.of(memberRecruiterA));
        when(memberRepository.findByCompanyIdAndUserId(999, 20)).thenReturn(Optional.empty());

        mockMvcCompany.perform(get("/api/companies/me?companyId=999")
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isForbidden());
    }

    // 6. GET /api/companies/me - Role RECRUITER không nhận dữ liệu nhạy cảm
    @Test
    @DisplayName("GET /api/companies/me - RECRUITER không nhận taxCode và businessLicenseUrl")
    void test_GetCompanyMe_Recruiter_HidesSensitiveLegalData() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);
        when(memberRepository.findByUserId(20)).thenReturn(List.of(memberRecruiterA));

        when(jobRepository.countByCompanyIdIn(any())).thenReturn(5L);
        when(jobRepository.countActiveByCompanyIds(any())).thenReturn(1L);
        when(applicationRepository.countByCompanyIds(any())).thenReturn(2L);
        when(memberRepository.countByCompanyId(1)).thenReturn(2L);
        when(locationRepository.countByCompanyId(1)).thenReturn(1L);

        mockMvcCompany.perform(get("/api/companies/me")
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("FPT Software"))
                .andExpect(jsonPath("$.currentUserMemberRole").value("RECRUITER"))
                .andExpect(jsonPath("$.taxCode").doesNotExist())
                .andExpect(jsonPath("$.businessLicenseUrl").doesNotExist());
    }

    // 7. PUT /api/companies/{id} - RECRUITER bị 403, COMPANY_ADMIN được sửa
    @Test
    @DisplayName("PUT /api/companies/{id} - RECRUITER bị 403 khi cố cập nhật hồ sơ công ty")
    void test_UpdateCompany_Recruiter_ThrowsForbidden() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 20, "COMPANY_ADMIN")).thenReturn(false);

        CompanyRequest req = new CompanyRequest();
        req.setCompanyName("FPT Updated");

        mockMvcCompany.perform(put("/api/companies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/companies/{id} - COMPANY_ADMIN cập nhật thành công")
    void test_UpdateCompany_CompanyAdmin_UpdatesSuccessfully() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 10, "COMPANY_ADMIN")).thenReturn(true);
        when(companyRepository.findById(1)).thenReturn(Optional.of(companyA));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyRequest req = new CompanyRequest();
        req.setCompanyName("FPT Software Global");
        req.setTaxCode("0101248141");

        mockMvcCompany.perform(put("/api/companies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("Authorization", "Bearer token-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("FPT Software Global"));
    }

    // 8. API công khai GET /api/companies/{id} không trả taxCode và businessLicenseUrl cho Candidate/Guest
    @Test
    @DisplayName("GET /api/companies/{id} - API công khai không làm lộ taxCode hay businessLicenseUrl cho Candidate/Guest")
    void test_GetCompanyDetail_PublicCandidate_DoesNotLeakSensitiveData() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(30); // Candidate id
        when(securityUtil.getCurrentUserRole(any())).thenReturn("CANDIDATE");
        when(companyRepository.findById(1)).thenReturn(Optional.of(companyA));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 30, "COMPANY_ADMIN")).thenReturn(false);

        mockMvcCompany.perform(get("/api/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("FPT Software"))
                .andExpect(jsonPath("$.taxCode").doesNotExist())
                .andExpect(jsonPath("$.businessLicenseUrl").doesNotExist())
                .andExpect(jsonPath("$.approvedByEmail").doesNotExist());
    }

    // 9. GET /api/companies/{id}/members - Kiểm tra quyền và email masking
    @Test
    @DisplayName("GET /api/companies/{id}/members - Không thuộc công ty trả 403")
    void test_GetCompanyMembers_NotMember_ThrowsForbidden() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(30);
        when(securityUtil.getCurrentUserRole(any())).thenReturn("CANDIDATE");
        when(memberRepository.findByCompanyIdAndUserId(1, 30)).thenReturn(Optional.empty());

        mockMvcCompany.perform(get("/api/companies/1/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/companies/{id}/members - RECRUITER xem danh sách thành viên bị mask email")
    void test_GetCompanyMembers_Recruiter_MasksEmail() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);
        when(securityUtil.getCurrentUserRole(any())).thenReturn("EMPLOYER");
        when(memberRepository.findByCompanyIdAndUserId(1, 20)).thenReturn(Optional.of(memberRecruiterA));
        when(memberRepository.findByCompanyId(1)).thenReturn(List.of(memberAdminA, memberRecruiterA));

        when(employerProfileRepository.findById(10)).thenReturn(Optional.empty());
        when(employerProfileRepository.findById(20)).thenReturn(Optional.empty());

        mockMvcCompany.perform(get("/api/companies/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(10))
                .andExpect(jsonPath("$[0].email").value("a***n@company.com")) // Masked
                .andExpect(jsonPath("$[1].userId").value(20))
                .andExpect(jsonPath("$[1].email").value("recruiter@company.com")); // Own email unmasked
    }

    @Test
    @DisplayName("GET /api/companies/{id}/members - COMPANY_ADMIN xem đầy đủ email thành viên")
    void test_GetCompanyMembers_CompanyAdmin_ShowsFullEmail() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);
        when(securityUtil.getCurrentUserRole(any())).thenReturn("EMPLOYER");
        when(memberRepository.findByCompanyIdAndUserId(1, 10)).thenReturn(Optional.of(memberAdminA));
        when(memberRepository.findByCompanyId(1)).thenReturn(List.of(memberAdminA, memberRecruiterA));

        when(employerProfileRepository.findById(10)).thenReturn(Optional.empty());
        when(employerProfileRepository.findById(20)).thenReturn(Optional.empty());

        mockMvcCompany.perform(get("/api/companies/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("admin@company.com"))
                .andExpect(jsonPath("$[1].email").value("recruiter@company.com"));
    }

    // 10. GET /api/companies/{id}/locations - Thành viên xem được, người ngoài bị 403
    @Test
    @DisplayName("GET /api/companies/{id}/locations - Thành viên công ty xem được danh sách địa điểm")
    void test_GetCompanyLocations_Member_ReturnsLocations() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);
        when(securityUtil.getCurrentUserRole(any())).thenReturn("EMPLOYER");
        when(memberRepository.findByCompanyIdAndUserId(1, 10)).thenReturn(Optional.of(memberAdminA));

        CompanyLocation loc = new CompanyLocation();
        loc.setId(50);
        loc.setCompany(companyA);
        loc.setLocationName("Trụ sở Cầu Giấy");
        loc.setAddress("Duy Tân, Cầu Giấy, Hà Nội");
        loc.setGeom(geometryFactory.createPoint(new Coordinate(105.78, 21.03)));
        loc.setPrimary(true);

        when(locationRepository.findByCompanyId(1)).thenReturn(List.of(loc));

        mockMvcLocation.perform(get("/api/companies/1/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].locationName").value("Trụ sở Cầu Giấy"))
                .andExpect(jsonPath("$[0].address").value("Duy Tân, Cầu Giấy, Hà Nội"))
                .andExpect(jsonPath("$[0].primary").value(true));
    }

    @Test
    @DisplayName("GET /api/companies/{id}/locations - Không phải thành viên bị 403")
    void test_GetCompanyLocations_NotMember_ThrowsForbidden() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(99);
        when(securityUtil.getCurrentUserRole(any())).thenReturn("CANDIDATE");
        when(memberRepository.findByCompanyIdAndUserId(1, 99)).thenReturn(Optional.empty());

        mockMvcLocation.perform(get("/api/companies/1/locations"))
                .andExpect(status().isForbidden());
    }
}
