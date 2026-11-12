package com.example.unipathapi.company.unit;

import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.company.dto.request.EmploymentInvitationCreateRequest;
import com.example.unipathapi.company.dto.response.EmploymentInvitationResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.EmploymentInvitation;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.repository.EmploymentInvitationRepository;
import com.example.unipathapi.company.repository.EmploymentRepository;
import com.example.unipathapi.company.service.CompanyManagementService;
import com.example.unipathapi.company.service.EmploymentInvitationService;
import com.example.unipathapi.job.entity.Job;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmploymentInvitationPhase1Test {

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
    private com.example.unipathapi.candidate.repository.CandidateProfileRepository candidateProfileRepository;

    @Mock
    private com.example.unipathapi.employer.repository.EmployerProfileRepository employerProfileRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private EmploymentInvitationService invitationService;

    @InjectMocks
    private CompanyManagementService companyManagementService;

    private Company companyHrm;
    private Company companyNoHrm;
    private User adminUser;
    private User recruiterUser;
    private User candidateUser;

    @BeforeEach
    void setUp() {
        companyHrm = new Company();
        companyHrm.setId(10);
        companyHrm.setCompanyName("Coffee Shop HRM");
        companyHrm.setHrmEnabled(true);

        companyNoHrm = new Company();
        companyNoHrm.setId(20);
        companyNoHrm.setCompanyName("Enterprise Corp No HRM");
        companyNoHrm.setHrmEnabled(false);

        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@coffeeshop.com");

        recruiterUser = new User();
        recruiterUser.setId(2);
        recruiterUser.setEmail("recruiter@coffeeshop.com");

        candidateUser = new User();
        candidateUser.setId(100);
        candidateUser.setEmail("candidate@student.vn");
    }

    @Test
    @DisplayName("createDirectInvitation: Success with HOURLY salary type")
    void testCreateDirectInvitation_Success_Hourly() {
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(100)).thenReturn(Optional.of(candidateUser));
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(100), anyList())).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(10, 100, "PENDING")).thenReturn(false);
        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(i -> {
            EmploymentInvitation inv = i.getArgument(0);
            inv.setId(1);
            return inv;
        });

        EmploymentInvitationCreateRequest req = new EmploymentInvitationCreateRequest();
        req.setCandidateId(100);
        req.setPosition("Pha chế Part-time");
        req.setSalaryType("HOURLY");
        req.setBaseSalaryPerHour(new BigDecimal("35000"));
        req.setStartDate(LocalDate.now().plusDays(3));

        EmploymentInvitationResponse res = invitationService.createDirectInvitation(1, 10, req);

        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        assertEquals("DIRECT", res.getInvitationType());
        assertEquals("HOURLY", res.getSalaryType());
        assertEquals(new BigDecimal("35000"), res.getBaseSalaryPerHour());
        assertNull(res.getBaseSalaryMonthly());
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createDirectInvitation: Success with MONTHLY salary type")
    void testCreateDirectInvitation_Success_Monthly() {
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(100)).thenReturn(Optional.of(candidateUser));
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(100), anyList())).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(10, 100, "PENDING")).thenReturn(false);
        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(i -> {
            EmploymentInvitation inv = i.getArgument(0);
            inv.setId(2);
            return inv;
        });

        EmploymentInvitationCreateRequest req = new EmploymentInvitationCreateRequest();
        req.setCandidateId(100);
        req.setPosition("Quản lý Ca Full-time");
        req.setSalaryType("MONTHLY");
        req.setBaseSalaryMonthly(new BigDecimal("12000000"));
        req.setStartDate(LocalDate.now().plusDays(5));

        EmploymentInvitationResponse res = invitationService.createDirectInvitation(1, 10, req);

        assertNotNull(res);
        assertEquals("PENDING", res.getStatus());
        assertEquals("MONTHLY", res.getSalaryType());
        assertEquals(new BigDecimal("12000000"), res.getBaseSalaryMonthly());
        assertNull(res.getBaseSalaryPerHour());
    }

    @Test
    @DisplayName("createDirectInvitation: Success with NEGOTIABLE and UNPAID salary types")
    void testCreateDirectInvitation_Success_NegotiableAndUnpaid() {
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(100)).thenReturn(Optional.of(candidateUser));
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(100), anyList())).thenReturn(false);
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(10, 100, "PENDING")).thenReturn(false);
        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(i -> i.getArgument(0));

        // NEGOTIABLE
        EmploymentInvitationCreateRequest req1 = new EmploymentInvitationCreateRequest();
        req1.setCandidateId(100);
        req1.setPosition("Thực tập sinh Marketing");
        req1.setSalaryType("NEGOTIABLE");
        req1.setSalaryNote("Thỏa thuận theo năng lực sau 1 tháng");
        req1.setStartDate(LocalDate.now().plusDays(7));

        EmploymentInvitationResponse res1 = invitationService.createDirectInvitation(1, 10, req1);
        assertEquals("NEGOTIABLE", res1.getSalaryType());
        assertEquals("Thỏa thuận theo năng lực sau 1 tháng", res1.getSalaryNote());

        // UNPAID
        EmploymentInvitationCreateRequest req2 = new EmploymentInvitationCreateRequest();
        req2.setCandidateId(100);
        req2.setPosition("Tình nguyện viên sự kiện");
        req2.setSalaryType("UNPAID");
        req2.setStartDate(LocalDate.now().plusDays(7));

        EmploymentInvitationResponse res2 = invitationService.createDirectInvitation(1, 10, req2);
        assertEquals("UNPAID", res2.getSalaryType());
    }

    @Test
    @DisplayName("createDirectInvitation: Throws 400 if company has HRM disabled")
    void testCreateDirectInvitation_CompanyHrmDisabled_ThrowsBadRequest() {
        when(companyRepository.findById(20)).thenReturn(Optional.of(companyNoHrm));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(20), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);

        EmploymentInvitationCreateRequest req = new EmploymentInvitationCreateRequest();
        req.setCandidateId(100);
        req.setPosition("Kế toán");
        req.setSalaryType("MONTHLY");
        req.setBaseSalaryMonthly(new BigDecimal("10000000"));
        req.setStartDate(LocalDate.now().plusDays(5));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            invitationService.createDirectInvitation(1, 20, req);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("chưa kích hoạt dịch vụ Quản lý nhân sự"));
    }

    @Test
    @DisplayName("createDirectInvitation: Throws 403 if user is RECRUITER (not COMPANY_ADMIN)")
    void testCreateDirectInvitation_NotCompanyAdmin_ThrowsForbidden() {
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(2), eq(List.of("COMPANY_ADMIN")))).thenReturn(false);

        EmploymentInvitationCreateRequest req = new EmploymentInvitationCreateRequest();
        req.setCandidateId(100);
        req.setPosition("Pha chế");
        req.setSalaryType("HOURLY");
        req.setBaseSalaryPerHour(new BigDecimal("30000"));
        req.setStartDate(LocalDate.now().plusDays(3));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            invitationService.createDirectInvitation(2, 10, req);
        });

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Chỉ Quản trị viên công ty (COMPANY_ADMIN)"));
    }

    @Test
    @DisplayName("createDirectInvitation: Throws 400 if Candidate already has ACTIVE or PENDING_START employment")
    void testCreateDirectInvitation_CandidateAlreadyEmployed_ThrowsBadRequest() {
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(100)).thenReturn(Optional.of(candidateUser));
        // Đã có hợp đồng nhân sự ACTIVE
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(100), anyList())).thenReturn(true);

        EmploymentInvitationCreateRequest req = new EmploymentInvitationCreateRequest();
        req.setCandidateId(100);
        req.setPosition("Pha chế");
        req.setSalaryType("HOURLY");
        req.setBaseSalaryPerHour(new BigDecimal("30000"));
        req.setStartDate(LocalDate.now().plusDays(3));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            invitationService.createDirectInvitation(1, 10, req);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Ứng viên đã có hợp đồng nhân sự đang hoạt động tại công ty"));
    }

    @Test
    @DisplayName("createDirectInvitation: Throws 400 if Candidate already has a PENDING invitation")
    void testCreateDirectInvitation_PendingInvitationExists_ThrowsBadRequest() {
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(100)).thenReturn(Optional.of(candidateUser));
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(100), anyList())).thenReturn(false);
        // Đang có lời mời PENDING
        when(invitationRepository.existsByCompanyIdAndCandidateIdAndStatus(10, 100, "PENDING")).thenReturn(true);

        EmploymentInvitationCreateRequest req = new EmploymentInvitationCreateRequest();
        req.setCandidateId(100);
        req.setPosition("Pha chế");
        req.setSalaryType("HOURLY");
        req.setBaseSalaryPerHour(new BigDecimal("30000"));
        req.setStartDate(LocalDate.now().plusDays(3));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            invitationService.createDirectInvitation(1, 10, req);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Đang có một lời mời chưa được phản hồi"));
    }

    @Test
    @DisplayName("createRecruitmentInvitation: Creates RECRUITMENT invitation when application is ACCEPTED")
    void testCreateRecruitmentInvitation_Success() {
        Job job = new Job();
        job.setId(5);
        job.setTitle("Nhân viên Bán hàng");
        job.setCompany(companyNoHrm); // Công ty không HRM vẫn tạo lời mời tuyển dụng bình thường

        Application app = new Application();
        app.setId(18);
        app.setJob(job);
        app.setCandidate(candidateUser);
        app.setStatus("ACCEPTED");

        when(invitationRepository.existsByApplicationId(18)).thenReturn(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(i -> {
            EmploymentInvitation inv = i.getArgument(0);
            inv.setId(180);
            return inv;
        });

        EmploymentInvitation saved = invitationService.createRecruitmentInvitation(
                app, 1, "Nhân viên Bán hàng", "HOURLY", new BigDecimal("32000"), null, null, LocalDate.now().plusDays(7), "Chào mừng gia nhập!"
        );

        assertNotNull(saved);
        assertEquals("RECRUITMENT", saved.getInvitationType());
        assertEquals("PENDING", saved.getStatus());
        assertEquals(app, saved.getApplication());
        assertEquals(new BigDecimal("32000"), saved.getBaseSalaryPerHour());
    }

    @Test
    @DisplayName("cancelInvitation: COMPANY_ADMIN can cancel PENDING invitation")
    void testCancelInvitation_Success() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);

        EmploymentInvitation inv = new EmploymentInvitation();
        inv.setId(50);
        inv.setCompany(companyHrm);
        inv.setCandidate(candidateUser);
        inv.setPosition("Pha chế");
        inv.setStatus("PENDING");

        when(invitationRepository.findById(50)).thenReturn(Optional.of(inv));
        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(i -> i.getArgument(0));

        EmploymentInvitationResponse res = invitationService.cancelInvitation(1, 10, 50);

        assertEquals("CANCELLED", res.getStatus());
        assertNotNull(res.getRespondedAt());
    }

    @Test
    @DisplayName("cancelInvitation: Throws 400 if invitation is not PENDING (cannot cancel ACCEPTED/DECLINED/EXPIRED)")
    void testCancelInvitation_NotPending_ThrowsBadRequest() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);

        EmploymentInvitation inv = new EmploymentInvitation();
        inv.setId(51);
        inv.setCompany(companyHrm);
        inv.setCandidate(candidateUser);
        inv.setStatus("ACCEPTED"); // Đã ACCEPT rồi thì không được phép CANCEL

        when(invitationRepository.findById(51)).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            invitationService.cancelInvitation(1, 10, 51);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Chỉ có thể hủy lời mời khi đang ở trạng thái PENDING"));
    }

    @Test
    @DisplayName("legacyCreateEmployment_Blocked: Calling legacy createEmployment throws 400 Bad Request")
    void testLegacyCreateEmployment_Blocked() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            companyManagementService.createEmployment(1, null);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Không thể tạo nhân viên trực tiếp ở trạng thái ACTIVE"));
    }
}
