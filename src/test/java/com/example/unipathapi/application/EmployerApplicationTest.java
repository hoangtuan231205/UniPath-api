package com.example.unipathapi.application;

import com.example.unipathapi.application.dto.response.ApplicationDetailResponse;
import com.example.unipathapi.application.dto.response.ApplicationResponse;
import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.application.service.ApplicationService;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.cv.entity.UserCvProfile;
import com.example.unipathapi.cv.repository.UserCvProfileRepository;
import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.job.repository.JobRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.application.dto.request.UpdateApplicationStatusRequest;
import com.example.unipathapi.notification.repository.NotificationRepository;
import com.example.unipathapi.notification.entity.Notification;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployerApplicationTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private UserCvProfileRepository userCvProfileRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.example.unipathapi.company.service.EmploymentInvitationService employmentInvitationService;

    @Mock
    private com.example.unipathapi.company.repository.EmploymentInvitationRepository invitationRepository;

    @Mock
    private com.example.unipathapi.company.repository.EmploymentRepository employmentRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private User candidateUser;
    private Company company;
    private Job job;
    private CandidateProfile candidateProfile;

    @BeforeEach
    void setUp() {
        candidateUser = new User();
        candidateUser.setId(101);
        candidateUser.setEmail("candidate1@example.com");

        company = new Company();
        company.setId(10);
        company.setCompanyName("TechCorp VN");

        job = new Job();
        job.setId(5);
        job.setTitle("Senior Java Developer");
        job.setCompany(company);

        candidateProfile = new CandidateProfile();
        candidateProfile.setId(101);
        candidateProfile.setUser(candidateUser);
        candidateProfile.setFullName("Nguyen Van A");
        candidateProfile.setPhoneNumber("0912345678");
        candidateProfile.setUniversityName("Đại học Bách Khoa");
        candidateProfile.setMajor("CNTT");
        candidateProfile.setSkills("Java, Spring, SQL");
        candidateProfile.setExperienceYears(3);
    }

    @Test
    @DisplayName("getApplicationsByJobId: batches candidate profile lookup and returns summary without N+1 queries")
    void testGetApplicationsByJobId_Success() {
        when(jobRepository.findById(5)).thenReturn(Optional.of(job));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList()))
                .thenReturn(true);

        Application app1 = new Application();
        app1.setId(1);
        app1.setJob(job);
        app1.setCandidate(candidateUser);
        app1.setStatus("SUBMITTED");
        app1.setCvFilename("resume.pdf");
        app1.setAppliedAt(LocalDateTime.now());

        Page<Application> appPage = new PageImpl<>(List.of(app1));
        when(applicationRepository.findByJobIdAndOptionalStatus(eq(5), isNull(), any(Pageable.class)))
                .thenReturn(appPage);

        when(candidateProfileRepository.findAllById(List.of(101)))
                .thenReturn(List.of(candidateProfile));

        Page<ApplicationResponse> responses = applicationService.getApplicationsByJobId(5, 99, null, 0, 10);

        assertNotNull(responses);
        assertEquals(1, responses.getContent().size());
        ApplicationResponse resp = responses.getContent().get(0);
        assertEquals("Nguyen Van A", resp.getCandidateName());
        assertEquals("candidate1@example.com", resp.getCandidateEmail());
        assertEquals("resume.pdf", resp.getCvFilename());

        // Verifies batch method was called exactly once for the whole page
        verify(candidateProfileRepository, times(1)).findAllById(List.of(101));
    }

    @Test
    @DisplayName("getApplicationDetail: returns full candidate profile, cover letter, and snapshot CV")
    void testGetApplicationDetail_WithCvSnapshot() {
        Application app = new Application();
        app.setId(17);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("SUBMITTED");
        app.setCoverLetter("Thư xin việc ấn tượng...");
        app.setCvSnapshotJson("{\"experience\": [\"FPT\", \"VNG\"]}");
        app.setCvProfileId(77);
        app.setAppliedAt(LocalDateTime.now());

        when(applicationRepository.findById(17)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList()))
                .thenReturn(true);
        when(candidateProfileRepository.findById(101)).thenReturn(Optional.of(candidateProfile));

        ApplicationDetailResponse detail = applicationService.getApplicationDetail(17, 99);

        assertNotNull(detail);
        assertEquals(17, detail.getId());
        assertEquals("Senior Java Developer", detail.getJobTitle());
        assertEquals("Nguyen Van A", detail.getCandidate().getFullName());
        assertEquals("0912345678", detail.getCandidate().getPhoneNumber());
        assertEquals("Đại học Bách Khoa", detail.getCandidate().getUniversityName());
        assertEquals("Thư xin việc ấn tượng...", detail.getCoverLetter());
        assertTrue(detail.getCv().getHasCvOnline());
        assertTrue(detail.getCv().getIsCvOnlineAvailable());
        assertEquals("{\"experience\": [\"FPT\", \"VNG\"]}", detail.getCv().getCvSnapshotJson());
        assertNull(detail.getCv().getCvOnlineStatusMessage());
    }

    @Test
    @DisplayName("getApplicationDetail: legacy application with deleted CV profile returns 200 with status message without failing")
    void testGetApplicationDetail_LegacyDeletedCvProfile_ReturnsFriendlyStatus() {
        Application app = new Application();
        app.setId(18);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("REVIEWING");
        app.setCoverLetter("Thư xin việc...");
        app.setCvSnapshotJson(null); // legacy application without snapshot
        app.setCvProfileId(88); // cv_profile was deleted
        app.setAppliedAt(LocalDateTime.now());

        when(applicationRepository.findById(18)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList()))
                .thenReturn(true);
        when(candidateProfileRepository.findById(101)).thenReturn(Optional.of(candidateProfile));
        when(userCvProfileRepository.findById(88)).thenReturn(Optional.empty()); // deleted!

        ApplicationDetailResponse detail = applicationService.getApplicationDetail(18, 99);

        assertNotNull(detail);
        assertEquals(18, detail.getId());
        assertEquals("Nguyen Van A", detail.getCandidate().getFullName());
        assertTrue(detail.getCv().getHasCvOnline());
        assertFalse(detail.getCv().getIsCvOnlineAvailable());
        assertEquals("CV Online đã nộp trước đây hiện không còn khả dụng.", detail.getCv().getCvOnlineStatusMessage());
    }

    @Test
    @DisplayName("getApplicationDetail: throws 403 Forbidden when employer does not belong to company")
    void testGetApplicationDetail_ForbiddenForNonCompanyMember() {
        Application app = new Application();
        app.setId(19);
        app.setJob(job);
        app.setCandidate(candidateUser);

        when(applicationRepository.findById(19)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(777), anyList()))
                .thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.getApplicationDetail(19, 777);
        });

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Bạn không có quyền xem hồ sơ"));
    }

    @Test
    @DisplayName("updateStatus: SUBMITTED -> REVIEWING succeeds and notifies candidate")
    void testUpdateStatus_SubmittedToReviewing_Success() {
        Application app = new Application();
        app.setId(20);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("SUBMITTED");

        when(applicationRepository.findById(20)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus("REVIEWING");
        req.setNote("Đang xét duyệt hồ sơ");

        ApplicationResponse res = applicationService.updateApplicationStatus(20, 99, req);

        assertNotNull(res);
        assertEquals("REVIEWING", res.getStatus());
        assertEquals("REVIEWING", app.getStatus());
        assertEquals("Đang xét duyệt hồ sơ", app.getNote());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("updateStatus: SUBMITTED -> INTERVIEW succeeds with valid datetime, location and notifies candidate")
    void testUpdateStatus_SubmittedToInterview_Success() {
        Application app = new Application();
        app.setId(21);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("SUBMITTED");

        when(applicationRepository.findById(21)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime interviewTime = LocalDateTime.now().plusDays(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus("INTERVIEW");
        req.setInterviewAt(interviewTime);
        req.setInterviewLocation("Văn phòng tầng 3");
        req.setNote("Mời phỏng vấn trực tiếp");

        ApplicationResponse res = applicationService.updateApplicationStatus(21, 99, req);

        assertNotNull(res);
        assertEquals("INTERVIEW", res.getStatus());
        assertEquals("INTERVIEW", app.getStatus());
        assertEquals(interviewTime, app.getInterviewAt());
        assertEquals("Văn phòng tầng 3", app.getInterviewLocation());
        assertEquals("Mời phỏng vấn trực tiếp", app.getNote());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("updateStatus: SUBMITTED -> ACCEPTED throws 400 Bad Request and does NOT notify")
    void testUpdateStatus_SubmittedToAccepted_ThrowsBadRequest() {
        Application app = new Application();
        app.setId(25);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("SUBMITTED");

        when(applicationRepository.findById(25)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus("ACCEPTED");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.updateApplicationStatus(25, 99, req);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Không thể chuyển trực tiếp từ SUBMITTED sang ACCEPTED"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("updateStatus: REVIEWING -> INTERVIEW succeeds with valid datetime and location, null note")
    void testUpdateStatus_ReviewingToInterview_Success() {
        Application app = new Application();
        app.setId(22);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("REVIEWING");

        when(applicationRepository.findById(22)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime interviewTime = LocalDateTime.now().plusDays(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus("INTERVIEW");
        req.setInterviewAt(interviewTime);
        req.setInterviewLocation("Tầng 5, Tòa nhà ABC");
        req.setNote(null); // empty note allowed

        ApplicationResponse res = applicationService.updateApplicationStatus(22, 99, req);

        assertNotNull(res);
        assertEquals("INTERVIEW", res.getStatus());
        assertEquals("INTERVIEW", app.getStatus());
        assertEquals(interviewTime, app.getInterviewAt());
        assertEquals("Tầng 5, Tòa nhà ABC", app.getInterviewLocation());
        assertNull(app.getNote());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("updateStatus: INTERVIEW without time or location throws 400 Bad Request")
    void testUpdateStatus_InterviewMissingFields_ThrowsBadRequest() {
        Application app = new Application();
        app.setId(23);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("REVIEWING");

        when(applicationRepository.findById(23)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus("INTERVIEW");
        req.setInterviewAt(null);
        req.setInterviewLocation("Văn phòng");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.updateApplicationStatus(23, 99, req);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Ngày giờ và địa điểm phỏng vấn là bắt buộc"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("updateStatus: INTERVIEW with time in the past throws 400 Bad Request")
    void testUpdateStatus_InterviewTimeInPast_ThrowsBadRequest() {
        Application app = new Application();
        app.setId(26);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("REVIEWING");

        when(applicationRepository.findById(26)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus("INTERVIEW");
        req.setInterviewAt(LocalDateTime.now().minusHours(2));
        req.setInterviewLocation("Văn phòng");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.updateApplicationStatus(26, 99, req);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Thời gian phỏng vấn không thể ở quá khứ"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("updateStatus: INTERVIEW with time exceeding 30 days throws 400 Bad Request")
    void testUpdateStatus_InterviewTimeExceeds30Days_ThrowsBadRequest() {
        Application app = new Application();
        app.setId(27);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("REVIEWING");

        when(applicationRepository.findById(27)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);

        UpdateApplicationStatusRequest req = new UpdateApplicationStatusRequest();
        req.setStatus("INTERVIEW");
        req.setInterviewAt(LocalDateTime.now().plusDays(31));
        req.setInterviewLocation("Văn phòng");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.updateApplicationStatus(27, 99, req);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("không được vượt quá 30 ngày"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("updateStatus: INTERVIEW -> ACCEPTED and INTERVIEW -> REJECTED succeed")
    void testUpdateStatus_InterviewToAcceptedAndRejected_Success() {
        Application app = new Application();
        app.setId(24);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("INTERVIEW");

        when(applicationRepository.findById(24)).thenReturn(Optional.of(app));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(99), anyList())).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateApplicationStatusRequest reqAccept = new UpdateApplicationStatusRequest();
        reqAccept.setStatus("ACCEPTED");
        reqAccept.setNote("Chúc mừng bạn đã trúng tuyển!");

        ApplicationResponse res = applicationService.updateApplicationStatus(24, 99, reqAccept);
        assertEquals("ACCEPTED", res.getStatus());

        // Now from terminal state ACCEPTED -> cannot transition further
        UpdateApplicationStatusRequest reqTerminal = new UpdateApplicationStatusRequest();
        reqTerminal.setStatus("REVIEWING");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            applicationService.updateApplicationStatus(24, 99, reqTerminal);
        });
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Không thể chuyển từ trạng thái cuối"));
    }
}
