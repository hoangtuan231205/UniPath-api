package com.example.unipathapi.job;

import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.application.service.ApplicationService;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyMember;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.job.dto.response.EmployerJobManagementResponse;
import com.example.unipathapi.job.dto.response.EmployerJobStatsResponse;
import com.example.unipathapi.job.dto.response.JobApplicationStatsDto;
import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.job.repository.JobRepository;
import com.example.unipathapi.job.repository.JobSkillRepository;
import com.example.unipathapi.job.repository.JobMediaRepository;
import com.example.unipathapi.job.repository.SavedJobRepository;
import com.example.unipathapi.job.service.JobService;
import com.example.unipathapi.user.entity.User;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmployerJobManagementTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private JobMediaRepository jobMediaRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private com.example.unipathapi.common.storage.FileStorageService fileStorageService;

    @InjectMocks
    private JobService jobService;

    @InjectMocks
    private ApplicationService applicationService;

    // =========================================================================
    // 1. Kiểm tra tính toán trạng thái thực tế (effectiveStatus)
    // =========================================================================
    @Test
    @DisplayName("1. calculateEffectiveStatus: ACTIVE, CLOSED, EXPIRED được tính chính xác")
    void testCalculateEffectiveStatus() {
        // Active and future expiration
        assertEquals("ACTIVE", JobService.calculateEffectiveStatus(true, LocalDateTime.now().plusDays(10)));
        assertEquals("ACTIVE", JobService.calculateEffectiveStatus(true, null));

        // Closed by employer and future expiration
        assertEquals("CLOSED", JobService.calculateEffectiveStatus(false, LocalDateTime.now().plusDays(10)));
        assertEquals("CLOSED", JobService.calculateEffectiveStatus(false, null));

        // Expired in past overrides isActive
        assertEquals("EXPIRED", JobService.calculateEffectiveStatus(true, LocalDateTime.now().minusDays(1)));
        assertEquals("EXPIRED", JobService.calculateEffectiveStatus(false, LocalDateTime.now().minusDays(1)));
    }

    // =========================================================================
    // 2. Nghiệp vụ mở lại tin tuyển dụng (reopenJob)
    // =========================================================================
    @Test
    @DisplayName("2. reopenJob: Từ chối mở lại khi tin đã quá hạn expiredAt và yêu cầu chỉnh sửa hạn trước")
    void testReopenJobExpiredThrowsBadRequest() {
        Integer jobId = 10;
        Integer userId = 100;
        Company company = new Company();
        company.setId(1);

        Job expiredJob = new Job();
        expiredJob.setId(jobId);
        expiredJob.setCompany(company);
        expiredJob.setIsActive(false);
        expiredJob.setExpiredAt(LocalDateTime.now().minusDays(2)); // Đã hết hạn

        CompanyMember member = new CompanyMember(company, new User(), "COMPANY_ADMIN");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(expiredJob));
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(member));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.reopenJob(jobId, userId));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("hết hạn"));
        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    @DisplayName("3. reopenJob: Cho phép mở lại khi expiredAt còn hiệu lực hoặc null")
    void testReopenJobSuccess() {
        Integer jobId = 10;
        Integer userId = 100;
        Company company = new Company();
        company.setId(1);

        Job closedJob = new Job();
        closedJob.setId(jobId);
        closedJob.setCompany(company);
        closedJob.setIsActive(false);
        closedJob.setExpiredAt(LocalDateTime.now().plusDays(15)); // Còn hạn

        CompanyMember member = new CompanyMember(company, new User(), "RECRUITER");

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(closedJob));
        when(memberRepository.findByUserId(userId)).thenReturn(List.of(member));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.getStatsForJobIds(anyCollection())).thenReturn(Collections.emptyList());
        when(jobSkillRepository.findByJobId(jobId)).thenReturn(Collections.emptyList());

        EmployerJobManagementResponse response = jobService.reopenJob(jobId, userId);

        assertNotNull(response);
        assertTrue(response.getIsActive());
        assertEquals("ACTIVE", response.getEffectiveStatus());
        verify(jobRepository).save(closedJob);
    }

    // =========================================================================
    // 3. Phân quyền xem ứng viên theo từng Job & Ngăn rò rỉ dữ liệu chéo công ty
    // =========================================================================
    @Test
    @DisplayName("4. getApplicationsByJobId: Chặn truy cập 403 Forbidden nếu Employer không thuộc công ty sở hữu Job")
    void testGetApplicationsByJobIdCrossCompanyForbidden() {
        Integer jobId = 50;
        Integer userId = 200;

        Company companyA = new Company();
        companyA.setId(1);

        Job jobCompanyA = new Job();
        jobCompanyA.setId(jobId);
        jobCompanyA.setCompany(companyA);

        // Giả lập Employer chỉ thuộc Company B (ID = 2)
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(jobCompanyA));
        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRoleIn(eq(1), eq(userId), anyCollection()))
                .thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> applicationService.getApplicationsByJobId(jobId, userId, null, 0, 10));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("không có quyền quản lý hồ sơ"));
    }

    @Test
    @DisplayName("5. getApplicationsByJobId: Trả về 404 khi không tìm thấy tin tuyển dụng")
    void testGetApplicationsByJobIdNotFound() {
        when(jobRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> applicationService.getApplicationsByJobId(999, 1, null, 0, 10));

        assertEquals(404, ex.getStatusCode().value());
    }

    // =========================================================================
    // 4. Batch query & Tính đúng số lượng ứng viên từng tin (Không N+1)
    // =========================================================================
    @Test
    @DisplayName("6. getEmployerJobs: Hỗ trợ multi-company, batch aggregation query tính đúng số hồ sơ mới nộp và phỏng vấn")
    void testGetEmployerJobsBatchQueryMetrics() {
        Integer userId = 100;
        Company company1 = new Company();
        company1.setId(1);
        Company company2 = new Company();
        company2.setId(2);

        CompanyMember m1 = new CompanyMember(company1, new User(), "COMPANY_ADMIN");
        CompanyMember m2 = new CompanyMember(company2, new User(), "RECRUITER");

        when(memberRepository.findByUserId(userId)).thenReturn(List.of(m1, m2));

        Job job1 = new Job();
        job1.setId(101);
        job1.setTitle("Backend Java Developer");
        job1.setCompany(company1);
        job1.setIsActive(true);

        Job job2 = new Job();
        job2.setId(102);
        job2.setTitle("Frontend React Developer");
        job2.setCompany(company2);
        job2.setIsActive(true);

        Page<Job> pagedJobs = new PageImpl<>(List.of(job1, job2));

        when(jobRepository.searchEmployerJobs(eq(List.of(1, 2)), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pagedJobs);

        // Giả lập 1 batch query trả về thống kê cho cả 2 jobs cùng lúc
        JobApplicationStatsDto stats1 = new JobApplicationStatsDto(101, 12L, 4L, 2L);
        JobApplicationStatsDto stats2 = new JobApplicationStatsDto(102, 8L, 2L, 1L);
        when(applicationRepository.getStatsForJobIds(anyCollection()))
                .thenReturn(List.of(stats1, stats2));

        Page<EmployerJobManagementResponse> result = jobService.getEmployerJobs(userId, null, null, "NEWEST", 0, 10);

        assertEquals(2, result.getTotalElements());

        // Kiểm tra Job 1
        EmployerJobManagementResponse r1 = result.getContent().get(0);
        assertEquals(101, r1.getId());
        assertEquals(12L, r1.getApplicationsCount());
        assertEquals(4L, r1.getNewApplicationsCount()); // SUBMITTED
        assertEquals(2L, r1.getInterviewApplicationsCount()); // INTERVIEW

        // Kiểm tra Job 2
        EmployerJobManagementResponse r2 = result.getContent().get(1);
        assertEquals(102, r2.getId());
        assertEquals(8L, r2.getApplicationsCount());
        assertEquals(2L, r2.getNewApplicationsCount()); // SUBMITTED
        assertEquals(1L, r2.getInterviewApplicationsCount()); // INTERVIEW

        // Xác nhận chỉ gọi duy nhất 1 lần batch query cho tất cả jobs trên page
        verify(applicationRepository, times(1)).getStatsForJobIds(List.of(101, 102));
    }

    // =========================================================================
    // 5. Thống kê tổng quan 4 thẻ đầu trang (Real Data)
    // =========================================================================
    @Test
    @DisplayName("7. getEmployerJobStats: Tính tổng số liệu thật 4 thẻ cho các công ty Employer quản lý")
    void testGetEmployerJobStats() {
        Integer userId = 100;
        Company company = new Company();
        company.setId(1);
        CompanyMember m = new CompanyMember(company, new User(), "COMPANY_ADMIN");

        when(memberRepository.findByUserId(userId)).thenReturn(List.of(m));
        when(jobRepository.countByCompanyIdIn(List.of(1))).thenReturn(12L);
        when(jobRepository.countActiveByCompanyIds(List.of(1))).thenReturn(7L);
        when(jobRepository.countClosedByCompanyIds(List.of(1))).thenReturn(5L);
        when(applicationRepository.countByCompanyIds(List.of(1))).thenReturn(48L);

        EmployerJobStatsResponse stats = jobService.getEmployerJobStats(userId);

        assertEquals(12L, stats.getTotalJobs());
        assertEquals(7L, stats.getActiveJobs());
        assertEquals(5L, stats.getClosedJobs());
        assertEquals(48L, stats.getTotalApplications());
    }
}
