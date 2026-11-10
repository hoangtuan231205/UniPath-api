package com.example.unipathapi.company;

import com.example.unipathapi.company.dto.response.EmployeeResponse;
import com.example.unipathapi.company.dto.response.EmploymentInvitationResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.Employment;
import com.example.unipathapi.company.entity.EmploymentInvitation;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.repository.EmploymentInvitationRepository;
import com.example.unipathapi.company.repository.EmploymentRepository;
import com.example.unipathapi.company.service.EmploymentInvitationService;
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
public class EmploymentInvitationPhase2Test {

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

    private Company companyHrm;
    private Company company2Hrm;
    private User adminUser;
    private User candidateUser;
    private User anotherCandidate;

    @BeforeEach
    void setUp() {
        companyHrm = new Company();
        companyHrm.setId(10);
        companyHrm.setCompanyName("Tech Corp");
        companyHrm.setHrmEnabled(true);

        company2Hrm = new Company();
        company2Hrm.setId(20);
        company2Hrm.setCompanyName("Coffee Shop");
        company2Hrm.setHrmEnabled(true);

        adminUser = new User();
        adminUser.setId(1);
        adminUser.setEmail("admin@techcorp.com");

        candidateUser = new User();
        candidateUser.setId(4);
        candidateUser.setEmail("candidate@unipath.local");

        anotherCandidate = new User();
        anotherCandidate.setId(99);
        anotherCandidate.setEmail("intruder@unipath.local");
    }

    private EmploymentInvitation createSampleInvitation(String status, LocalDate startDate, LocalDateTime expiresAt) {
        EmploymentInvitation inv = new EmploymentInvitation();
        inv.setId(100);
        inv.setCompany(companyHrm);
        inv.setCandidate(candidateUser);
        inv.setInvitedBy(adminUser);
        inv.setInvitationType("DIRECT");
        inv.setStatus(status);
        inv.setPosition("Backend Java Developer");
        inv.setSalaryType("MONTHLY");
        inv.setBaseSalaryMonthly(new BigDecimal("15000000"));
        inv.setStartDate(startDate);
        inv.setExpiresAt(expiresAt);
        return inv;
    }

    @Test
    @DisplayName("Candidate ACCEPT: Cập nhật ACCEPTED, lưu respondedAt, gửi thông báo, KHÔNG tạo Employment")
    void testCandidateAccept_Success() {
        EmploymentInvitation inv = createSampleInvitation("PENDING", LocalDate.now().plusDays(5), LocalDateTime.now().plusDays(3));
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(i -> i.getArgument(0));

        EmploymentInvitationResponse response = invitationService.acceptInvitation(4, 100);

        assertEquals("ACCEPTED", response.getStatus());
        assertNotNull(response.getRespondedAt());
        verify(notificationRepository, times(1)).save(any(Notification.class));
        // Kiểm tra chắc chắn: KHÔNG gọi tạo Employment ở bước này!
        verify(employmentRepository, never()).save(any(Employment.class));
    }

    @Test
    @DisplayName("Candidate DECLINE: Cập nhật DECLINED, lưu declineReason, KHÔNG tạo Employment")
    void testCandidateDecline_Success() {
        EmploymentInvitation inv = createSampleInvitation("PENDING", LocalDate.now().plusDays(5), LocalDateTime.now().plusDays(3));
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(invitationRepository.save(any(EmploymentInvitation.class))).thenAnswer(i -> i.getArgument(0));

        EmploymentInvitationResponse response = invitationService.declineInvitation(4, 100, "Đã tìm được công việc khác phù hợp hơn");

        assertEquals("DECLINED", response.getStatus());
        assertEquals("Đã tìm được công việc khác phù hợp hơn", response.getDeclineReason());
        assertNotNull(response.getRespondedAt());
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(employmentRepository, never()).save(any(Employment.class));
    }

    @Test
    @DisplayName("Candidate không thể phản hồi lời mời của người khác -> HTTP 403 Forbidden")
    void testCandidateCannotRespondOtherCandidateInvitation() {
        EmploymentInvitation inv = createSampleInvitation("PENDING", LocalDate.now().plusDays(5), LocalDateTime.now().plusDays(3));
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));

        ResponseStatusException exAccept = assertThrows(ResponseStatusException.class, () ->
                invitationService.acceptInvitation(99, 100));
        assertEquals(403, exAccept.getStatusCode().value());

        ResponseStatusException exDecline = assertThrows(ResponseStatusException.class, () ->
                invitationService.declineInvitation(99, 100, "Lý do"));
        assertEquals(403, exDecline.getStatusCode().value());
    }

    @Test
    @DisplayName("Employer không thể Onboard trước khi Candidate đồng ý (Status != ACCEPTED) -> HTTP 400")
    void testEmployerCannotOnboardBeforeCandidateAccepts() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        EmploymentInvitation inv = createSampleInvitation("PENDING", LocalDate.now().plusDays(5), LocalDateTime.now().plusDays(3));
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.onboardCandidate(1, 10, 100));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("ACCEPTED"));
    }

    @Test
    @DisplayName("Employer không thể Onboard vào công ty khác (Sai companyId) -> HTTP 403 Forbidden")
    void testEmployerCannotOnboardToDifferentCompany() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(20), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        EmploymentInvitation inv = createSampleInvitation("ACCEPTED", LocalDate.now().plusDays(5), LocalDateTime.now().plusDays(3));
        // Lời mời thuộc công ty 10 nhưng Employer truyền companyId 20
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.onboardCandidate(1, 20, 100));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("không thuộc công ty"));
    }

    @Test
    @DisplayName("Onboarding trùng request (Đã onboard từ invitation này trước đó) -> HTTP 400")
    void testOnboardingDuplicateRequest_ThrowsBadRequest() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        EmploymentInvitation inv = createSampleInvitation("ACCEPTED", LocalDate.now().plusDays(5), LocalDateTime.now().plusDays(3));
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(employmentRepository.existsByInvitationId(100)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.onboardCandidate(1, 10, 100));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("trước đó"));
    }

    @Test
    @DisplayName("Lời mời đã hết hạn (expiresAt trong quá khứ) -> Chuyển EXPIRED và ném HTTP 400")
    void testExpiredInvitation_CannotRespond() {
        EmploymentInvitation inv = createSampleInvitation("PENDING", LocalDate.now().plusDays(5), LocalDateTime.now().minusDays(1));
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.acceptInvitation(4, 100));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("hết hạn"));
        assertEquals("EXPIRED", inv.getStatus());
        verify(invitationRepository, atLeastOnce()).save(inv);
    }

    @Test
    @DisplayName("Candidate được phép làm việc tại nhiều công ty khác nhau (Company 1 ACTIVE, onboard Company 2 thành công)")
    void testCandidateCanWorkAtMultipleCompanies() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(20), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);

        EmploymentInvitation inv2 = new EmploymentInvitation();
        inv2.setId(200);
        inv2.setCompany(company2Hrm);
        inv2.setCandidate(candidateUser);
        inv2.setInvitedBy(adminUser);
        inv2.setStatus("ACCEPTED");
        inv2.setPosition("Pha chế part-time");
        inv2.setSalaryType("HOURLY");
        inv2.setBaseSalaryPerHour(new BigDecimal("30000"));
        inv2.setStartDate(LocalDate.now());

        when(invitationRepository.findById(200)).thenReturn(Optional.of(inv2));
        when(companyRepository.findById(20)).thenReturn(Optional.of(company2Hrm));
        when(employmentRepository.existsByInvitationId(200)).thenReturn(false);
        // Tại công ty 2, ứng viên CHƯA có employment (dù công ty 1 có thể đã có)
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(20), eq(4), anyList())).thenReturn(false);
        when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> {
            Employment e = i.getArgument(0);
            e.setId(555);
            return e;
        });

        EmployeeResponse res = invitationService.onboardCandidate(1, 20, 200);

        assertNotNull(res);
        assertEquals(20, res.getCompanyId());
        assertEquals("ACTIVE", res.getStatus());
        assertEquals("Pha chế part-time", res.getPosition());
    }

    @Test
    @DisplayName("Employment có start_date trong tương lai -> Tự động thiết lập PENDING_START")
    void testEmploymentWithFutureStartDate_SetsPendingStart() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        LocalDate futureStartDate = LocalDate.now().plusDays(10);
        EmploymentInvitation inv = createSampleInvitation("ACCEPTED", futureStartDate, LocalDateTime.now().plusDays(3));

        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(employmentRepository.existsByInvitationId(100)).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(4), anyList())).thenReturn(false);
        when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> {
            Employment e = i.getArgument(0);
            e.setId(888);
            return e;
        });

        EmployeeResponse res = invitationService.onboardCandidate(1, 10, 100);

        assertEquals("PENDING_START", res.getStatus());
        assertEquals(futureStartDate, res.getStartDate());
    }

    @Test
    @DisplayName("Bảo toàn điều khoản bất biến: Lương, vị trí và ngày bắt đầu trong Employment khớp 100% với lời mời")
    void testOnboardPreservesImmutableTerms() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        LocalDate expectedStartDate = LocalDate.now().plusDays(3);
        EmploymentInvitation inv = createSampleInvitation("ACCEPTED", expectedStartDate, LocalDateTime.now().plusDays(3));
        inv.setPosition("Senior Backend Engineer");
        inv.setSalaryType("MONTHLY");
        inv.setBaseSalaryMonthly(new BigDecimal("25000000"));

        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(employmentRepository.existsByInvitationId(100)).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(4), anyList())).thenReturn(false);
        when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> {
            Employment e = i.getArgument(0);
            e.setId(999);
            return e;
        });

        EmployeeResponse res = invitationService.onboardCandidate(1, 10, 100);

        assertEquals("Senior Backend Engineer", res.getPosition());
        assertEquals("MONTHLY", res.getSalaryType());
        assertEquals(new BigDecimal("25000000"), res.getBaseSalaryMonthly());
        assertEquals(expectedStartDate, res.getStartDate());
    }

    @Test
    @DisplayName("activateDueEmployments: Chuyển PENDING_START sang ACTIVE khi đã đến hoặc qua ngày bắt đầu")
    void testActivateDueEmployments() {
        Employment emp1 = new Employment();
        emp1.setId(1);
        emp1.setStatus("PENDING_START");
        emp1.setStartDate(LocalDate.now());

        Employment emp2 = new Employment();
        emp2.setId(2);
        emp2.setStatus("PENDING_START");
        emp2.setStartDate(LocalDate.now().minusDays(1));

        when(employmentRepository.findByStatusAndStartDateLessThanEqual(eq("PENDING_START"), eq(LocalDate.now())))
                .thenReturn(List.of(emp1, emp2));

        int activatedCount = invitationService.activateDueEmployments();

        assertEquals(2, activatedCount);
        assertEquals("ACTIVE", emp1.getStatus());
        assertEquals("ACTIVE", emp2.getStatus());
        verify(employmentRepository, times(2)).save(any(Employment.class));
    }

    @Test
    @DisplayName("HRM bật sau khi Candidate ACCEPT: Khi công ty bật HRM, COMPANY_ADMIN onboard thành công không cần ứng tuyển lại")
    void testHrmEnabledAfterCandidateAccepts_CanOnboardSuccessfully() {
        // Giả sử ban đầu công ty chưa bật HRM
        Company companyInitiallyNoHrm = new Company();
        companyInitiallyNoHrm.setId(10);
        companyInitiallyNoHrm.setCompanyName("Tech Corp");
        companyInitiallyNoHrm.setHrmEnabled(false);

        // Candidate đã ACCEPT lời mời
        EmploymentInvitation inv = createSampleInvitation("ACCEPTED", LocalDate.now(), LocalDateTime.now().plusDays(3));
        inv.setCompany(companyInitiallyNoHrm);

        // Sau đó công ty bật HRM
        companyInitiallyNoHrm.setHrmEnabled(true);

        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyInitiallyNoHrm));
        when(employmentRepository.existsByInvitationId(100)).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(4), anyList())).thenReturn(false);
        when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> {
            Employment e = i.getArgument(0);
            e.setId(777);
            return e;
        });

        EmployeeResponse res = invitationService.onboardCandidate(1, 10, 100);

        assertNotNull(res);
        assertEquals("ACTIVE", res.getStatus());
        assertEquals(10, res.getCompanyId());
    }

    @Test
    @DisplayName("Request đồng thời: Khi Database ném DataIntegrityViolationException, API trả về HTTP 400 thay vì 500")
    void testOnboardingDataIntegrityViolation_ThrowsBadRequestInsteadOf500() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        EmploymentInvitation inv = createSampleInvitation("ACCEPTED", LocalDate.now(), LocalDateTime.now().plusDays(3));

        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(employmentRepository.existsByInvitationId(100)).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(4), anyList())).thenReturn(false);

        // Giả lập 2 request chạy song song: pass tầng check Java nhưng tầng DB ném DataIntegrityViolationException do UNIQUE index
        when(employmentRepository.save(any(Employment.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key value violates unique constraint"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invitationService.onboardCandidate(1, 10, 100));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("đang hoạt động hoặc chuẩn bị bắt đầu"));
    }

    @Test
    @DisplayName("Lương NEGOTIABLE: Điều khoản lương NEGOTIABLE được bảo toàn bất biến trong Employment")
    void testNegotiableSalary_PreservedOnOnboarding() {
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(10), eq(1), eq(List.of("COMPANY_ADMIN")))).thenReturn(true);
        EmploymentInvitation inv = createSampleInvitation("ACCEPTED", LocalDate.now(), LocalDateTime.now().plusDays(3));
        inv.setSalaryType("NEGOTIABLE");
        inv.setBaseSalaryPerHour(null);
        inv.setBaseSalaryMonthly(null);

        when(invitationRepository.findById(100)).thenReturn(Optional.of(inv));
        when(companyRepository.findById(10)).thenReturn(Optional.of(companyHrm));
        when(employmentRepository.existsByInvitationId(100)).thenReturn(false);
        when(employmentRepository.existsByCompanyIdAndCandidateIdAndStatusIn(eq(10), eq(4), anyList())).thenReturn(false);
        when(employmentRepository.save(any(Employment.class))).thenAnswer(i -> {
            Employment e = i.getArgument(0);
            e.setId(666);
            return e;
        });

        EmployeeResponse res = invitationService.onboardCandidate(1, 10, 100);

        assertEquals("NEGOTIABLE", res.getSalaryType());
        assertNull(res.getBaseSalaryPerHour());
        assertNull(res.getBaseSalaryMonthly());
    }
}
