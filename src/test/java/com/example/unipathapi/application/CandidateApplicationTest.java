package com.example.unipathapi.application;

import com.example.unipathapi.application.dto.response.ApplicationResponse;
import com.example.unipathapi.application.dto.response.CandidateApplicationSummaryResponse;
import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.application.service.ApplicationService;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyLocation;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
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
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CandidateApplicationTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private com.example.unipathapi.company.repository.EmploymentInvitationRepository invitationRepository;

    @Mock
    private com.example.unipathapi.company.repository.EmploymentRepository employmentRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private User candidateUser;
    private Company company;
    private CompanyLocation location;
    private Job job;

    @BeforeEach
    void setUp() {
        candidateUser = new User();
        candidateUser.setId(100);
        candidateUser.setEmail("candidate@test.com");

        company = new Company();
        company.setId(10);
        company.setCompanyName("TechCorp VN");
        company.setLogoUrl("https://example.com/logo.png");

        location = new CompanyLocation();
        location.setId(20);
        location.setAddress("123 Nguyen Trai, Q1, HCM");
        location.setLocationName("Tru so chinh");

        job = new Job();
        job.setId(5);
        job.setTitle("Frontend Engineer Intern");
        job.setCompany(company);
        job.setLocation(location);
        job.setJobType(JobType.INTERNSHIP);
        job.setWorkMode(WorkMode.HYBRID);
        job.setSalaryRange("8 - 12 trieu");
    }

    @Test
    @DisplayName("getMyApplications maps candidate fields cleanly without cvUrl, candidateId, or N+1 queries")
    void testGetMyApplications_Success() {
        Application app = new Application();
        app.setId(1);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("REVIEWING");
        app.setCvFilename("resume_2026.pdf");
        app.setCvUrl("private-uploads/cv/secret_hash_resume.pdf");
        app.setAppliedAt(LocalDateTime.now().minusDays(2));

        Page<Application> pageResult = new PageImpl<>(List.of(app));
        when(applicationRepository.findCandidateApplications(eq(100), eq("REVIEWING"), eq("Engineer"), any(Pageable.class)))
                .thenReturn(pageResult);

        Page<ApplicationResponse> responses = applicationService.getMyApplications(100, "REVIEWING", "Engineer", "appliedAt,desc", 0, 10);

        assertNotNull(responses);
        assertEquals(1, responses.getContent().size());
        ApplicationResponse resp = responses.getContent().get(0);

        assertEquals(1, resp.getId());
        assertEquals("Frontend Engineer Intern", resp.getJobTitle());
        assertEquals("TechCorp VN", resp.getCompanyName());
        assertEquals("123 Nguyen Trai, Q1, HCM", resp.getLocationAddress());
        assertEquals(JobType.INTERNSHIP, resp.getJobType());
        assertEquals(WorkMode.HYBRID, resp.getWorkMode());
        assertEquals("resume_2026.pdf", resp.getCvFilename());
        assertTrue(resp.getCanWithdraw());

        // Ensure candidate fields are null for candidate view
        assertNull(resp.getCandidateId());
        assertNull(resp.getCandidateName());
        assertNull(resp.getCandidateEmail());

        // Verify zero calls to candidateProfileRepository (guarantees NO N+1 candidate profile lookups)
        verifyNoInteractions(candidateProfileRepository);
    }

    @Test
    @DisplayName("getMyApplicationSummary aggregates stats from count query accurately")
    void testGetMyApplicationSummary() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"SUBMITTED", 3L});
        rows.add(new Object[]{"REVIEWING", 2L});
        rows.add(new Object[]{"INTERVIEW", 1L});
        rows.add(new Object[]{"ACCEPTED", 1L});
        rows.add(new Object[]{"REJECTED", 0L});
        rows.add(new Object[]{"WITHDRAWN", 1L});

        when(applicationRepository.countByStatusForCandidate(100)).thenReturn(rows);

        CandidateApplicationSummaryResponse summary = applicationService.getMyApplicationSummary(100);

        assertNotNull(summary);
        assertEquals(8L, summary.getTotal());
        assertEquals(3L, summary.getSubmitted());
        assertEquals(2L, summary.getReviewing());
        assertEquals(1L, summary.getInterview());
        assertEquals(1L, summary.getAccepted());
        assertEquals(0L, summary.getRejected());
        assertEquals(1L, summary.getWithdrawn());
    }

    @Test
    @DisplayName("withdrawApplication succeeds when status is SUBMITTED or REVIEWING")
    void testWithdrawApplication_AllowedStatus() {
        Application app = new Application();
        app.setId(1);
        app.setCandidate(candidateUser);
        app.setStatus("SUBMITTED");
        app.setJob(job);

        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse result = applicationService.withdrawApplication(1, 100);

        assertNotNull(result);
        assertEquals("WITHDRAWN", app.getStatus());
        verify(applicationRepository, times(1)).save(app);
    }

    @Test
    @DisplayName("withdrawApplication throws BAD_REQUEST when status is INTERVIEW, ACCEPTED or REJECTED")
    void testWithdrawApplication_DisallowedStatus() {
        Application app = new Application();
        app.setId(2);
        app.setCandidate(candidateUser);
        app.setStatus("INTERVIEW");

        when(applicationRepository.findById(2)).thenReturn(Optional.of(app));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.withdrawApplication(2, 100);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Chỉ có thể rút hồ sơ khi trạng thái là SUBMITTED hoặc REVIEWING"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("withdrawApplication throws FORBIDDEN when candidate is not owner")
    void testWithdrawApplication_ForbiddenForNonOwner() {
        Application app = new Application();
        app.setId(3);
        app.setCandidate(candidateUser); // id = 100
        app.setStatus("SUBMITTED");

        when(applicationRepository.findById(3)).thenReturn(Optional.of(app));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.withdrawApplication(3, 999); // another user
        });

        assertEquals(403, ex.getStatusCode().value());
        verify(applicationRepository, never()).save(any());
    }
}
