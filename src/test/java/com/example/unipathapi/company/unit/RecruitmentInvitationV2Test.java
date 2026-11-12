package com.example.unipathapi.company.unit;

import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.company.dto.request.RecruitmentInvitationCreateRequest;
import com.example.unipathapi.company.dto.response.EmploymentInvitationResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.Employment;
import com.example.unipathapi.company.entity.EmploymentInvitation;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.repository.EmploymentInvitationRepository;
import com.example.unipathapi.company.repository.EmploymentRepository;
import com.example.unipathapi.company.service.EmploymentInvitationService;
import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.notification.entity.Notification;
import com.example.unipathapi.notification.repository.NotificationRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecruitmentInvitationV2Test {

    @Mock
    private EmploymentInvitationRepository invitationRepository;

    @Mock
    private EmploymentRepository employmentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private com.example.unipathapi.candidate.repository.CandidateProfileRepository candidateProfileRepository;

    @Mock
    private com.example.unipathapi.employer.repository.EmployerProfileRepository employerProfileRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private EmploymentInvitationService invitationService;

    private Company company;
    private User employerUser;
    private User candidateUser;
    private Job job;
    private Application acceptedApp;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(5);
        company.setCompanyName("FPT Software");
        company.setHrmEnabled(true);

        employerUser = new User();
        employerUser.setId(10);
        employerUser.setEmail("admin@fpt.com");

        candidateUser = new User();
        candidateUser.setId(20);
        candidateUser.setEmail("candidate@fpt.com");

        job = new Job();
        job.setId(21);
        job.setTitle("Frontend Developer");
        job.setCompany(company);

        acceptedApp = new Application();
        acceptedApp.setId(18);
        acceptedApp.setJob(job);
        acceptedApp.setCandidate(candidateUser);
        acceptedApp.setStatus("ACCEPTED");
        acceptedApp.setAppliedAt(LocalDateTime.now().minusDays(3));
    }

    private RecruitmentInvitationCreateRequest createValidRequest() {
        RecruitmentInvitationCreateRequest req = new RecruitmentInvitationCreateRequest();
        req.setPosition("Frontend Developer");
        req.setSalaryType("MONTHLY");
        req.setBaseSalaryMonthly(new BigDecimal("15000000"));
        req.setStartDate(LocalDate.now().plusDays(7));
        req.setNote("Chào mừng bạn gia nhập đội ngũ!");
        return req;
    }

    @Test
    @DisplayName("Scenario 1: RECRUITER is forbidden (403) from sending recruitment invitation")
    void testRecruiterCannotSendInvitation() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(false);

        RecruitmentInvitationCreateRequest req = createValidRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, req)
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Chỉ Quản trị viên công ty"));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 2: COMPANY_ADMIN of another company is forbidden (403)")
    void testAdminOfAnotherCompanyForbidden() {
        when(companyRepository.findById(99)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(99, 10, List.of("COMPANY_ADMIN"))).thenReturn(false);

        RecruitmentInvitationCreateRequest req = createValidRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 99, 18, req)
        );

        assertEquals(403, ex.getStatusCode().value());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 3: Non-ACCEPTED application throws 400 Bad Request")
    void testNonAcceptedApplicationThrowsBadRequest() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);

        Application reviewingApp = new Application();
        reviewingApp.setId(19);
        reviewingApp.setJob(job);
        reviewingApp.setCandidate(candidateUser);
        reviewingApp.setStatus("REVIEWING");

        when(applicationRepository.findById(19)).thenReturn(Optional.of(reviewingApp));

        RecruitmentInvitationCreateRequest req = createValidRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 19, req)
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Chỉ có thể gửi lời mời tuyển dụng cho hồ sơ đã được duyệt (ACCEPTED)"));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 4: Application does not exist throws 404 Not Found")
    void testApplicationNotFoundThrows404() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);
        when(applicationRepository.findById(999)).thenReturn(Optional.empty());

        RecruitmentInvitationCreateRequest req = createValidRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 999, req)
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 5 & 6: Active PENDING/ACCEPTED invitation already exists on Application throws 409 Conflict")
    void testActivePendingOrAcceptedInvitationExistsThrows409() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);
        when(applicationRepository.findById(18)).thenReturn(Optional.of(acceptedApp));
        when(invitationRepository.existsByApplicationIdAndStatusIn(18, List.of("PENDING", "ACCEPTED"))).thenReturn(true);

        RecruitmentInvitationCreateRequest req = createValidRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, req)
        );

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("đang chờ phản hồi hoặc đã được chấp thuận"));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 7: Candidate has ACTIVE or PENDING_START employment at company throws 400 Bad Request")
    void testCandidateActiveEmploymentThrows400() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);
        when(applicationRepository.findById(18)).thenReturn(Optional.of(acceptedApp));
        when(invitationRepository.existsByApplicationIdAndStatusIn(18, List.of("PENDING", "ACCEPTED"))).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(5, 20, List.of("ACTIVE", "PENDING_START"))).thenReturn(true);

        RecruitmentInvitationCreateRequest req = createValidRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, req)
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("đã là nhân viên đang hoạt động hoặc chuẩn bị nhận việc"));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 8: Start date in past or invalid monthly salary throws 400")
    void testInvalidSalaryOrPastStartDateThrows400() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);
        when(applicationRepository.findById(18)).thenReturn(Optional.of(acceptedApp));
        when(invitationRepository.existsByApplicationIdAndStatusIn(18, List.of("PENDING", "ACCEPTED"))).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(5, 20, List.of("ACTIVE", "PENDING_START"))).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(5, 20, "PENDING")).thenReturn(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(employerUser));

        // Past start date
        RecruitmentInvitationCreateRequest reqPastDate = createValidRequest();
        reqPastDate.setStartDate(LocalDate.now().minusDays(1));

        ResponseStatusException exDate = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, reqPastDate)
        );
        assertEquals(400, exDate.getStatusCode().value());
        assertTrue(exDate.getReason().contains("không thể ở quá khứ"));

        // Non-positive salary
        RecruitmentInvitationCreateRequest reqZeroSalary = createValidRequest();
        reqZeroSalary.setBaseSalaryMonthly(BigDecimal.ZERO);

        ResponseStatusException exSalary = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, reqZeroSalary)
        );
        assertEquals(400, exSalary.getStatusCode().value());
        assertTrue(exSalary.getReason().contains("lớn hơn 0"));
    }

    @Test
    @DisplayName("Scenario 9: Successful invitation creation creates PENDING invitation and notification")
    void testSuccessfulInvitationCreation() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);
        when(applicationRepository.findById(18)).thenReturn(Optional.of(acceptedApp));
        when(invitationRepository.existsByApplicationIdAndStatusIn(18, List.of("PENDING", "ACCEPTED"))).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(5, 20, List.of("ACTIVE", "PENDING_START"))).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(5, 20, "PENDING")).thenReturn(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(employerUser));

        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(invocation -> {
            EmploymentInvitation inv = invocation.getArgument(0);
            inv.setId(999);
            return inv;
        });

        RecruitmentInvitationCreateRequest req = createValidRequest();
        EmploymentInvitationResponse resp = invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, req);

        assertNotNull(resp);
        assertEquals(999, resp.getId());
        assertEquals("PENDING", resp.getStatus());
        assertEquals(5, resp.getCompanyId());
        assertEquals(20, resp.getCandidateId());
        assertEquals(18, resp.getApplicationId());
        assertEquals("Frontend Developer", resp.getPosition());
        assertEquals("RECRUITMENT", resp.getInvitationType());

        // Verify notification was sent to candidate
        verify(notificationRepository, times(1)).save(argThat(n ->
                n.getUser().getId().equals(20) &&
                n.getMessage().contains("lời mời nhận việc") &&
                n.getTitle().contains("Lời mời nhận việc")
        ));
    }

    @Test
    @DisplayName("Scenario 10: Previous invitation DECLINED/CANCELLED/EXPIRED allows re-invitation and preserves history")
    void testReinvitationAfterDeclinedAllowsNewInvitation() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);
        when(applicationRepository.findById(18)).thenReturn(Optional.of(acceptedApp));

        // When checking existsByApplicationIdAndStatusIn(18, PENDING/ACCEPTED) -> returns false because existing is DECLINED
        when(invitationRepository.existsByApplicationIdAndStatusIn(18, List.of("PENDING", "ACCEPTED"))).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(5, 20, List.of("ACTIVE", "PENDING_START"))).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(5, 20, "PENDING")).thenReturn(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(employerUser));

        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(invocation -> {
            EmploymentInvitation inv = invocation.getArgument(0);
            inv.setId(1001);
            return inv;
        });

        RecruitmentInvitationCreateRequest req = createValidRequest();
        req.setBaseSalaryMonthly(new BigDecimal("18000000")); // increased offer

        EmploymentInvitationResponse resp = invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, req);

        assertNotNull(resp);
        assertEquals(1001, resp.getId());
        assertEquals("PENDING", resp.getStatus());
        assertEquals(new BigDecimal("18000000"), resp.getBaseSalaryMonthly());
    }

    @Test
    @DisplayName("Scenario 11: Concurrent duplicate throws 409 Conflict when unique index constraint is violated")
    void testConcurrentDuplicateThrows409() {
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(5, 10, List.of("COMPANY_ADMIN"))).thenReturn(true);
        when(applicationRepository.findById(18)).thenReturn(Optional.of(acceptedApp));
        when(invitationRepository.existsByApplicationIdAndStatusIn(18, List.of("PENDING", "ACCEPTED"))).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(5, 20, List.of("ACTIVE", "PENDING_START"))).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(5, 20, "PENDING")).thenReturn(false);
        when(userRepository.findById(10)).thenReturn(Optional.of(employerUser));

        when(invitationRepository.save(any(EmploymentInvitation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uq_invitation_application_active"));

        RecruitmentInvitationCreateRequest req = createValidRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.createRecruitmentInvitationFromApplication(10, 5, 18, req)
        );

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Lời mời làm việc cho hồ sơ này đã được tạo hoặc đang được xử lý đồng thời"));
        verify(notificationRepository, never()).save(any());
    }
}
