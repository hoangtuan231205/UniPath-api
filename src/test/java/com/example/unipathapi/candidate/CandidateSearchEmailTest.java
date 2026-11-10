package com.example.unipathapi.candidate;

import com.example.unipathapi.candidate.controller.CandidateSearchController;
import com.example.unipathapi.candidate.dto.response.CandidateSearchResponse;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.candidate.service.CandidateSearchService;
import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CandidateSearchEmailTest {

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private CandidateSearchService candidateSearchService;

    @InjectMocks
    private CandidateSearchController candidateSearchController;

    private MockMvc mockMvc;

    private CandidateProfile candidate1;
    private CandidateProfile candidate2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(candidateSearchController, "candidateSearchService", candidateSearchService);
        mockMvc = MockMvcBuilders.standaloneSetup(candidateSearchController).build();

        User u1 = new User();
        u1.setId(10);
        u1.setEmail("hoangtuan@unipath.vn");
        u1.setRole("CANDIDATE");
        u1.setIsActive(true);

        candidate1 = new CandidateProfile();
        candidate1.setId(10);
        candidate1.setUser(u1);
        candidate1.setFullName("Hoàng Anh Tuấn");
        candidate1.setUniversityName("Đại học Bách Khoa");
        candidate1.setMajor("Công nghệ thông tin");
        candidate1.setSkills("Java, Spring Boot, React");
        candidate1.setPhoneNumber("0987654321");

        User u2 = new User();
        u2.setId(20);
        u2.setEmail("linh.tran@gmail.com");
        u2.setRole("CANDIDATE");
        u2.setIsActive(true);

        candidate2 = new CandidateProfile();
        candidate2.setId(20);
        candidate2.setUser(u2);
        candidate2.setFullName("Trần Thùy Linh");
        candidate2.setUniversityName("Đại học Kinh Tế Quốc Dân");
        candidate2.setMajor("Marketing");
        candidate2.setSkills("Content, SEO, Photoshop");
        candidate2.setPhoneNumber("0912345678");
    }

    @Test
    @DisplayName("Email detection regex matches valid email format and rejects non-email keywords")
    void testIsEmailFormat() {
        assertTrue(CandidateSearchService.isEmailFormat("candidate@unipath.vn"));
        assertTrue(CandidateSearchService.isEmailFormat("TEST.USER+123@GMAIL.COM"));
        assertTrue(CandidateSearchService.isEmailFormat("student@edu.vn"));

        assertFalse(CandidateSearchService.isEmailFormat("Hoàng Anh Tuấn"));
        assertFalse(CandidateSearchService.isEmailFormat("Bách Khoa"));
        assertFalse(CandidateSearchService.isEmailFormat("Java, Spring Boot"));
        assertFalse(CandidateSearchService.isEmailFormat("tuan@"));
        assertFalse(CandidateSearchService.isEmailFormat("@unipath.vn"));
        assertFalse(CandidateSearchService.isEmailFormat("tuan@unipath"));
        assertFalse(CandidateSearchService.isEmailFormat(""));
        assertFalse(CandidateSearchService.isEmailFormat(null));
    }

    @Test
    @DisplayName("When keyword is valid email, executes exact email repository query at database level")
    void testExactEmailSearchCallsRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CandidateProfile> page = new PageImpl<>(List.of(candidate1), pageable, 1);

        when(candidateProfileRepository.findByExactEmail(eq("hoangtuan@unipath.vn"), any(Pageable.class)))
                .thenReturn(page);

        Page<CandidateSearchResponse> result = candidateSearchService.searchCandidates(null, null, null, "hoangtuan@unipath.vn", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        CandidateSearchResponse response = result.getContent().get(0);
        assertEquals("Hoàng Anh Tuấn", response.getFullName());
        assertEquals(10, response.getUserId());
        // Email must be masked
        assertEquals("h***@unipath.vn", response.getEmail());
        assertEquals("098****321", response.getPhoneNumber());

        verify(candidateProfileRepository, times(1)).findByExactEmail(eq("hoangtuan@unipath.vn"), any(Pageable.class));
        verify(candidateProfileRepository, never()).searchProfiles(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("When keyword is not email, executes searchProfiles repository query across name, univ, skills")
    void testStandardKeywordSearchCallsSearchProfiles() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CandidateProfile> page = new PageImpl<>(List.of(candidate1), pageable, 1);

        when(candidateProfileRepository.searchProfiles(isNull(), isNull(), isNull(), eq("Bách Khoa"), any(Pageable.class)))
                .thenReturn(page);

        Page<CandidateSearchResponse> result = candidateSearchService.searchCandidates(null, null, null, "Bách Khoa", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Đại học Bách Khoa", result.getContent().get(0).getUniversityName());

        verify(candidateProfileRepository, times(1)).searchProfiles(isNull(), isNull(), isNull(), eq("Bách Khoa"), any(Pageable.class));
        verify(candidateProfileRepository, never()).findByExactEmail(any(), any());
    }

    @Test
    @DisplayName("When exact email does not exist, returns empty page gracefully without exposing errors")
    void testNonExistentEmailReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CandidateProfile> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(candidateProfileRepository.findByExactEmail(eq("unknown@unipath.vn"), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<CandidateSearchResponse> result = candidateSearchService.searchCandidates(null, null, null, "unknown@unipath.vn", 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("API GET /api/candidates/search with exact email requires COMPANY_ADMIN and masks contact data")
    void testControllerExactEmailSearchWithSecurity() throws Exception {
        when(securityUtil.getCurrentUserId(any())).thenReturn(99);
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(1, 99, "COMPANY_ADMIN")).thenReturn(true);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CandidateProfile> page = new PageImpl<>(List.of(candidate1), pageable, 1);
        when(candidateProfileRepository.findByExactEmail(eq("hoangtuan@unipath.vn"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/candidates/search")
                        .param("companyId", "1")
                        .param("keyword", "hoangtuan@unipath.vn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId", is(10)))
                .andExpect(jsonPath("$.content[0].fullName", is("Hoàng Anh Tuấn")))
                .andExpect(jsonPath("$.content[0].email", is("h***@unipath.vn")))
                .andExpect(jsonPath("$.content[0].phoneNumber", is("098****321")));
    }
}
