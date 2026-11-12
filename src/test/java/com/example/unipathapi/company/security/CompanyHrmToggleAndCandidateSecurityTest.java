package com.example.unipathapi.company.security;

import com.example.unipathapi.candidate.controller.CandidateSearchController;
import com.example.unipathapi.candidate.dto.response.CandidateSearchResponse;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.candidate.service.CandidateSearchService;
import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.company.controller.CompanyController;
import com.example.unipathapi.company.dto.request.CompanyHrmStatusRequest;
import com.example.unipathapi.company.dto.response.CandidateEmploymentResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.EmploymentInvitation;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.repository.EmploymentInvitationRepository;
import com.example.unipathapi.company.repository.EmploymentRepository;
import com.example.unipathapi.company.service.CompanyManagementService;
import com.example.unipathapi.company.service.EmploymentInvitationService;
import com.example.unipathapi.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CompanyHrmToggleAndCandidateSecurityTest {

    private MockMvc mockMvcCompany;
    private MockMvc mockMvcCandidateSearch;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private EmploymentRepository employmentRepository;

    @Mock
    private EmploymentInvitationRepository invitationRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private CompanyManagementService companyManagementService;

    @InjectMocks
    private EmploymentInvitationService invitationService;

    @InjectMocks
    private CandidateSearchService candidateSearchService;

    private CompanyController companyController;
    private CandidateSearchController candidateSearchController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        companyController = new CompanyController();
        ReflectionTestUtils.setField(companyController, "companyService", companyManagementService);
        ReflectionTestUtils.setField(companyController, "invitationService", invitationService);
        ReflectionTestUtils.setField(companyController, "securityUtil", securityUtil);
        mockMvcCompany = MockMvcBuilders.standaloneSetup(companyController).build();

        candidateSearchController = new CandidateSearchController();
        ReflectionTestUtils.setField(candidateSearchController, "candidateSearchService", candidateSearchService);
        ReflectionTestUtils.setField(candidateSearchController, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(candidateSearchController, "securityUtil", securityUtil);
        mockMvcCandidateSearch = MockMvcBuilders.standaloneSetup(candidateSearchController).build();
    }

    // =========================================================================
    // SECTION 1: HRM TOGGLE API & AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/employer/companies/{companyId}/hrm-status: COMPANY_ADMIN có quyền bật HRM thành công")
    void testUpdateCompanyHrmStatus_CompanyAdmin_EnableSuccess() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);

        Company company = new Company();
        company.setId(1);
        company.setCompanyName("Tech Corp");
        company.setHrmEnabled(false);

        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 10, "COMPANY_ADMIN")).thenReturn(true);
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));

        CompanyHrmStatusRequest request = new CompanyHrmStatusRequest(true);

        mockMvcCompany.perform(patch("/api/employer/companies/1/hrm-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.companyName").value("Tech Corp"))
                .andExpect(jsonPath("$.hrmEnabled").value(true));

        assertTrue(company.getHrmEnabled());
        verify(companyRepository, times(1)).save(company);
    }

    @Test
    @DisplayName("PATCH /api/employer/companies/{companyId}/hrm-status: RECRUITER bị từ chối với HTTP 403 Forbidden")
    void testUpdateCompanyHrmStatus_Recruiter_Forbidden() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);

        Company company = new Company();
        company.setId(1);
        company.setCompanyName("Tech Corp");
        company.setHrmEnabled(true);

        when(companyRepository.findById(1)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 20, "COMPANY_ADMIN")).thenReturn(false);

        CompanyHrmStatusRequest request = new CompanyHrmStatusRequest(false);

        mockMvcCompany.perform(patch("/api/employer/companies/1/hrm-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("COMPANY_ADMIN")));

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("PATCH /api/employer/companies/{companyId}/hrm-status: COMPANY_ADMIN tắt HRM mà không làm mất dữ liệu lịch sử")
    void testUpdateCompanyHrmStatus_DisablePreservesData() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);

        Company company = new Company();
        company.setId(2);
        company.setCompanyName("Retail Corp");
        company.setHrmEnabled(true);

        when(companyRepository.findById(2)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(2, 10, "COMPANY_ADMIN")).thenReturn(true);
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));

        CompanyHrmStatusRequest request = new CompanyHrmStatusRequest(false);

        mockMvcCompany.perform(patch("/api/employer/companies/2/hrm-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hrmEnabled").value(false));

        assertFalse(company.getHrmEnabled());
        // Verify: Không có thao tác xóa bảng hay sửa đổi dữ liệu employment/shifts
        verify(employmentRepository, never()).deleteAll(any());
    }

    // =========================================================================
    // SECTION 2: CANDIDATE SEARCH SECURITY, PERMISSION & DATA MASKING
    // =========================================================================

    @Test
    @DisplayName("GET /api/candidates/search: Thiếu companyId trả về HTTP 400 Bad Request")
    void testCandidateSearch_MissingCompanyId_ReturnsBadRequest() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);

        mockMvcCandidateSearch.perform(get("/api/candidates/search")
                        .param("keyword", "Java"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("companyId")));
    }

    @Test
    @DisplayName("GET /api/candidates/search: Người gọi không phải COMPANY_ADMIN của companyId trả về HTTP 403 Forbidden")
    void testCandidateSearch_NotCompanyAdmin_ReturnsForbidden() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(20);
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 20, "COMPANY_ADMIN")).thenReturn(false);

        mockMvcCandidateSearch.perform(get("/api/candidates/search")
                        .param("companyId", "1")
                        .param("keyword", "Java"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("COMPANY_ADMIN")));
    }

    @Test
    @DisplayName("GET /api/candidates/search: COMPANY_ADMIN tìm kiếm thành công, che số điện thoại và email")
    void testCandidateSearch_CompanyAdmin_DataMasked() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(10);
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 10, "COMPANY_ADMIN")).thenReturn(true);

        User candidateUser = new User();
        candidateUser.setId(101);
        candidateUser.setEmail("nguyenvana@gmail.com");

        CandidateProfile profile = new CandidateProfile();
        profile.setId(101);
        profile.setUser(candidateUser);
        profile.setFullName("Nguyễn Văn A");
        profile.setPhoneNumber("0987654321");
        profile.setUniversityName("Đại học Bách Khoa");
        profile.setMajor("Công nghệ thông tin");
        profile.setSkills("Java, Spring Boot");

        when(candidateProfileRepository.findAll()).thenReturn(List.of(profile));

        mockMvcCandidateSearch.perform(get("/api/candidates/search")
                        .param("companyId", "1")
                        .param("keyword", "Java")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(101))
                .andExpect(jsonPath("$.content[0].fullName").value("Nguyễn Văn A"))
                // Che số điện thoại: "098****321" thay vì "0987654321"
                .andExpect(jsonPath("$.content[0].phoneNumber").value("098****321"))
                // Che email: "n***@gmail.com" thay vì "nguyenvana@gmail.com"
                .andExpect(jsonPath("$.content[0].email").value("n***@gmail.com"));
    }

    @Test
    @DisplayName("GET /api/candidates/search: Rate limit kích hoạt khi vượt quá 30 requests/phút -> HTTP 429")
    void testCandidateSearch_RateLimiting_Exceeded() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(99);
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 99, "COMPANY_ADMIN")).thenReturn(true);
        when(candidateProfileRepository.findAll()).thenReturn(Collections.emptyList());

        // Thực hiện 30 requests hợp lệ
        for (int i = 0; i < 30; i++) {
            mockMvcCandidateSearch.perform(get("/api/candidates/search")
                            .param("companyId", "1")
                            .param("keyword", "Dev"))
                    .andExpect(status().isOk());
        }

        // Request thứ 31 bị chặn với HTTP 429 Too Many Requests
        mockMvcCandidateSearch.perform(get("/api/candidates/search")
                        .param("companyId", "1")
                        .param("keyword", "Dev"))
                .andExpect(status().isTooManyRequests());
    }

    // =========================================================================
    // SECTION 3: CANDIDATE ACCEPTED OFFERS WITHOUT EMPLOYMENT (NON-HRM COMPATIBILITY)
    // =========================================================================

    @Test
    @DisplayName("getCandidateEmployments: Candidate xem được lời mời ACCEPTED ở công ty chưa bật HRM dù chưa có Employment")
    void testGetCandidateEmployments_IncludesAcceptedOfferWithoutEmployment() {
        when(employmentRepository.findByCandidateIdAndStatusIn(eq(4), anyList())).thenReturn(Collections.emptyList());

        Company nonHrmCompany = new Company();
        nonHrmCompany.setId(99);
        nonHrmCompany.setCompanyName("Coffee Shop Local");
        nonHrmCompany.setHrmEnabled(false);

        EmploymentInvitation acceptedInv = new EmploymentInvitation();
        acceptedInv.setId(77);
        acceptedInv.setCompany(nonHrmCompany);
        acceptedInv.setStatus("ACCEPTED");
        acceptedInv.setPosition("Phục vụ bàn");
        acceptedInv.setSalaryType("HOURLY");
        acceptedInv.setBaseSalaryPerHour(new BigDecimal("25000"));
        acceptedInv.setStartDate(LocalDate.now().plusDays(3));

        when(invitationRepository.findByCandidateIdAndStatus(4, "ACCEPTED")).thenReturn(List.of(acceptedInv));
        when(employmentRepository.existsByInvitationId(77)).thenReturn(false);

        List<CandidateEmploymentResponse> responses = invitationService.getCandidateEmployments(4);

        assertEquals(1, responses.size());
        CandidateEmploymentResponse res = responses.get(0);
        assertNull(res.getEmploymentId());
        assertEquals(77, res.getInvitationId());
        assertFalse(res.getIsEmployment());
        assertEquals("ACCEPTED_OFFER", res.getStatus());
        assertEquals("Coffee Shop Local", res.getCompanyName());
        assertFalse(res.getHrmEnabled());
        assertEquals("Phục vụ bàn", res.getPosition());
        assertEquals(new BigDecimal("25000"), res.getBaseSalaryPerHour());
    }
}
