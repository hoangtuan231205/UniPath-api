package com.example.unipathapi.job;

import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyMember;
import com.example.unipathapi.company.repository.CompanyLocationRepository;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.job.dto.request.JobRequest;
import com.example.unipathapi.job.dto.response.EmployerJobManagementResponse;
import com.example.unipathapi.job.dto.response.JobApplicationStatsDto;
import com.example.unipathapi.job.dto.response.JobResponse;
import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.service.JobService;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobTypeAndWorkModeTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @Mock
    private JobLikeRepository jobLikeRepository;

    @Mock
    private JobCommentRepository jobCommentRepository;

    @Mock
    private JobShareRepository jobShareRepository;

    @Mock
    private JobMediaRepository jobMediaRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private com.example.unipathapi.common.storage.FileStorageService fileStorageService;

    @InjectMocks
    private JobService jobService;

    private Validator validator;
    private User mockUser;
    private Company mockCompany;
    private CompanyMember mockMember;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setEmail("recruiter@example.com");

        mockCompany = new Company();
        mockCompany.setId(10);
        mockCompany.setCompanyName("UniPath Corp");
        mockCompany.setStatus("APPROVED");

        mockMember = new CompanyMember();
        mockMember.setUser(mockUser);
        mockMember.setCompany(mockCompany);
        mockMember.setMemberRole("RECRUITER");
    }

    @Test
    @DisplayName("1. Tạo tin với FULL_TIME và ONSITE")
    void testCreateJob_FullTime_Onsite() {
        when(memberRepository.findByUserId(1)).thenReturn(List.of(mockMember));
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        JobRequest request = new JobRequest();
        request.setTitle("Senior Java Developer");
        request.setJobType(JobType.FULL_TIME);
        request.setWorkMode(WorkMode.ONSITE);
        request.setSalaryRange("20-30M");

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(101);
            return j;
        });

        JobResponse response = jobService.createJob(1, request);
        assertNotNull(response);
        assertEquals(JobType.FULL_TIME, response.getJobType());
        assertEquals(WorkMode.ONSITE, response.getWorkMode());

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals(JobType.FULL_TIME, captor.getValue().getJobType());
        assertEquals(WorkMode.ONSITE, captor.getValue().getWorkMode());
    }

    @Test
    @DisplayName("2. Tạo tin với INTERNSHIP và HYBRID")
    void testCreateJob_Internship_Hybrid() {
        when(memberRepository.findByUserId(1)).thenReturn(List.of(mockMember));
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        JobRequest request = new JobRequest();
        request.setTitle("Java Backend Intern");
        request.setJobType(JobType.INTERNSHIP);
        request.setWorkMode(WorkMode.HYBRID);

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(102);
            return j;
        });

        JobResponse response = jobService.createJob(1, request);
        assertNotNull(response);
        assertEquals(JobType.INTERNSHIP, response.getJobType());
        assertEquals(WorkMode.HYBRID, response.getWorkMode());
    }

    @Test
    @DisplayName("3. Tạo tin với CONTRACT và REMOTE")
    void testCreateJob_Contract_Remote() {
        when(memberRepository.findByUserId(1)).thenReturn(List.of(mockMember));
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        JobRequest request = new JobRequest();
        request.setTitle("Contract DevOps Engineer");
        request.setJobType(JobType.CONTRACT);
        request.setWorkMode(WorkMode.REMOTE);

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(103);
            return j;
        });

        JobResponse response = jobService.createJob(1, request);
        assertNotNull(response);
        assertEquals(JobType.CONTRACT, response.getJobType());
        assertEquals(WorkMode.REMOTE, response.getWorkMode());
    }

    @Test
    @DisplayName("4. Thiếu jobType bị vi phạm validation @NotNull")
    void testValidation_MissingJobType() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setWorkMode(WorkMode.ONSITE);
        // jobType is null

        Set<ConstraintViolation<JobRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("jobType")));
    }

    @Test
    @DisplayName("5. Thiếu workMode bị vi phạm validation @NotNull")
    void testValidation_MissingWorkMode() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setJobType(JobType.FULL_TIME);
        // workMode is null

        Set<ConstraintViolation<JobRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("workMode")));
    }

    @Test
    @DisplayName("6. Gửi jobType = INTERN bị ném IllegalArgumentException khi parse Enum")
    void testInvalidJobType_InternRejected() {
        assertThrows(IllegalArgumentException.class, () -> JobType.valueOf("INTERN"));
        assertDoesNotThrow(() -> JobType.valueOf("INTERNSHIP"));
    }

    @Test
    @DisplayName("7. Gửi workMode = ONLINE bị ném IllegalArgumentException khi parse Enum")
    void testInvalidWorkMode_OnlineRejected() {
        assertThrows(IllegalArgumentException.class, () -> WorkMode.valueOf("ONLINE"));
        assertDoesNotThrow(() -> WorkMode.valueOf("REMOTE"));
    }

    @Test
    @DisplayName("8. Chỉnh sửa riêng jobType không làm mất workMode")
    void testUpdateJob_OnlyJobType_PreservesWorkMode() {
        Job existingJob = new Job();
        existingJob.setId(50);
        existingJob.setCompany(mockCompany);
        existingJob.setTitle("Backend Engineer");
        existingJob.setJobType(JobType.FULL_TIME);
        existingJob.setWorkMode(WorkMode.HYBRID);

        when(jobRepository.findById(50)).thenReturn(Optional.of(existingJob));
        when(memberRepository.findByCompanyIdAndUserId(10, 1)).thenReturn(Optional.of(mockMember));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        JobRequest updateReq = new JobRequest();
        updateReq.setTitle("Backend Engineer");
        updateReq.setJobType(JobType.CONTRACT); // change to CONTRACT
        updateReq.setWorkMode(null); // workMode not provided in partial update

        JobResponse res = jobService.updateJob(50, 1, updateReq);
        assertEquals(JobType.CONTRACT, res.getJobType());
        assertEquals(WorkMode.HYBRID, res.getWorkMode()); // preserved!
    }

    @Test
    @DisplayName("9. Chỉnh sửa riêng workMode không làm mất jobType")
    void testUpdateJob_OnlyWorkMode_PreservesJobType() {
        Job existingJob = new Job();
        existingJob.setId(50);
        existingJob.setCompany(mockCompany);
        existingJob.setTitle("Backend Engineer");
        existingJob.setJobType(JobType.FULL_TIME);
        existingJob.setWorkMode(WorkMode.HYBRID);

        when(jobRepository.findById(50)).thenReturn(Optional.of(existingJob));
        when(memberRepository.findByCompanyIdAndUserId(10, 1)).thenReturn(Optional.of(mockMember));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        JobRequest updateReq = new JobRequest();
        updateReq.setTitle("Backend Engineer");
        updateReq.setJobType(null); // jobType not provided in partial update
        updateReq.setWorkMode(WorkMode.REMOTE); // change to REMOTE

        JobResponse res = jobService.updateJob(50, 1, updateReq);
        assertEquals(JobType.FULL_TIME, res.getJobType()); // preserved!
        assertEquals(WorkMode.REMOTE, res.getWorkMode());
    }

    @Test
    @DisplayName("10. Response trả về đủ cả hai field trong JobResponse và EmployerJobManagementResponse")
    void testResponsesContainBothFields() {
        Job job = new Job();
        job.setId(99);
        job.setCompany(mockCompany);
        job.setTitle("Frontend Dev");
        job.setJobType(JobType.PART_TIME);
        job.setWorkMode(WorkMode.REMOTE);
        job.setIsActive(true);

        JobResponse jobRes = jobService.buildJobResponse(job);
        assertEquals(JobType.PART_TIME, jobRes.getJobType());
        assertEquals(WorkMode.REMOTE, jobRes.getWorkMode());

        JobApplicationStatsDto statsDto = new JobApplicationStatsDto(99, 10L, 2L, 1L);
        EmployerJobManagementResponse empRes = jobService.buildEmployerJobResponse(job, statsDto);
        assertEquals(JobType.PART_TIME, empRes.getJobType());
        assertEquals(WorkMode.REMOTE, empRes.getWorkMode());
    }

    @Test
    @DisplayName("11. Lọc feed chỉ theo jobType")
    void testFeedFilter_OnlyJobType() {
        when(jobRepository.findFeedJobs(isNull(), isNull(), isNull(), isNull(), eq(JobType.PART_TIME), isNull(), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        List<JobResponse> result = jobService.getFeedJobs(null, null, null, null, "PART_TIME", null, null);
        assertNotNull(result);
        verify(jobRepository).findFeedJobs(isNull(), isNull(), isNull(), isNull(), eq(JobType.PART_TIME), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("12. Lọc feed chỉ theo workMode")
    void testFeedFilter_OnlyWorkMode() {
        when(jobRepository.findFeedJobs(isNull(), isNull(), isNull(), isNull(), isNull(), eq(WorkMode.REMOTE), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        List<JobResponse> result = jobService.getFeedJobs(null, null, null, null, null, "REMOTE", null);
        assertNotNull(result);
        verify(jobRepository).findFeedJobs(isNull(), isNull(), isNull(), isNull(), isNull(), eq(WorkMode.REMOTE), any(Pageable.class));
    }

    @Test
    @DisplayName("13. Lọc feed đồng thời theo cả jobType và workMode")
    void testFeedFilter_BothJobTypeAndWorkMode() {
        when(jobRepository.findFeedJobs(isNull(), isNull(), isNull(), isNull(), eq(JobType.INTERNSHIP), eq(WorkMode.HYBRID), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        List<JobResponse> result = jobService.getFeedJobs(null, null, null, null, "INTERNSHIP", "HYBRID", null);
        assertNotNull(result);
        verify(jobRepository).findFeedJobs(isNull(), isNull(), isNull(), isNull(), eq(JobType.INTERNSHIP), eq(WorkMode.HYBRID), any(Pageable.class));
    }

    @Test
    @DisplayName("14. Hai bộ lọc kết hợp đúng với keyword và cursor")
    void testFeedFilter_CombinedWithKeywordAndCursor() {
        when(jobRepository.findFeedJobs(eq(500), eq("Developer"), isNull(), isNull(), eq(JobType.FULL_TIME), eq(WorkMode.ONSITE), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        List<JobResponse> result = jobService.getFeedJobs(500, "Developer", null, null, "FULL_TIME", "ONSITE", null);
        assertNotNull(result);
        verify(jobRepository).findFeedJobs(eq(500), eq("Developer"), isNull(), isNull(), eq(JobType.FULL_TIME), eq(WorkMode.ONSITE), any(Pageable.class));
    }

    @Test
    @DisplayName("15. Chuẩn hóa requirements: chuỗi văn bản thường được chuyển thành JSON string hợp lệ")
    void testNormalizeRequirements_PlainText() {
        String input = "không yêu cầu kinh nghiệm";
        String normalized = JobService.normalizeRequirements(input);
        assertNotNull(normalized);
        assertEquals("\"không yêu cầu kinh nghiệm\"", normalized);

        String formattedBack = JobService.formatRequirementsForResponse(normalized);
        assertEquals(input, formattedBack);
    }

    @Test
    @DisplayName("16. Chuẩn hóa requirements: JSON object/array được giữ nguyên")
    void testNormalizeRequirements_JsonObjectAndArray() {
        String jsonObject = "{\"skills\": [\"Java\", \"Spring Boot\"], \"experience\": \"1 năm\"}";
        String normalizedObj = JobService.normalizeRequirements(jsonObject);
        assertEquals(jsonObject, normalizedObj);
        assertEquals(jsonObject, JobService.formatRequirementsForResponse(normalizedObj));

        String jsonArray = "[\"Java\", \"PostgreSQL\"]";
        String normalizedArr = JobService.normalizeRequirements(jsonArray);
        assertEquals(jsonArray, normalizedArr);
        assertEquals(jsonArray, JobService.formatRequirementsForResponse(normalizedArr));
    }

    @Test
    @DisplayName("17. Chuẩn hóa requirements: văn bản nhiều dòng và ký tự đặc biệt được escape an toàn")
    void testNormalizeRequirements_Multiline() {
        String multiline = "- Tốt nghiệp đại học\n- Có kinh nghiệm \"thực chiến\" Java\n- Giao tiếp tốt";
        String normalized = JobService.normalizeRequirements(multiline);
        assertTrue(normalized.startsWith("\"") && normalized.endsWith("\""));
        assertTrue(normalized.contains("\\n"));
        assertTrue(normalized.contains("\\\"thực chiến\\\""));

        String formatted = JobService.formatRequirementsForResponse(normalized);
        assertEquals(multiline, formatted);
    }

    @Test
    @DisplayName("18. Chuẩn hóa requirements: null hoặc rỗng trả về null")
    void testNormalizeRequirements_NullOrEmpty() {
        assertNull(JobService.normalizeRequirements(null));
        assertNull(JobService.normalizeRequirements("   "));
        assertNull(JobService.formatRequirementsForResponse(null));
        assertNull(JobService.formatRequirementsForResponse("   "));
    }

    @Test
    @DisplayName("19. expiredAt ở quá khứ bị vi phạm validation @Future")
    void testValidation_ExpiredAtInPastThrowsViolation() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setJobType(JobType.FULL_TIME);
        request.setWorkMode(WorkMode.ONSITE);
        request.setExpiredAt(LocalDateTime.now().minusDays(1)); // Quá khứ

        Set<ConstraintViolation<JobRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("expiredAt") &&
                v.getMessage().equals("Hạn ứng tuyển không được ở quá khứ")));
    }

    @Test
    @DisplayName("20. expiredAt ở tương lai hoặc null hợp lệ khi validate")
    void testValidation_ExpiredAtInFutureOrNullIsValid() {
        JobRequest requestFuture = new JobRequest();
        requestFuture.setTitle("Java Developer");
        requestFuture.setJobType(JobType.FULL_TIME);
        requestFuture.setWorkMode(WorkMode.ONSITE);
        requestFuture.setExpiredAt(LocalDateTime.now().plusDays(7)); // Tương lai

        Set<ConstraintViolation<JobRequest>> violationsFuture = validator.validate(requestFuture);
        assertTrue(violationsFuture.isEmpty());

        JobRequest requestNull = new JobRequest();
        requestNull.setTitle("Java Developer");
        requestNull.setJobType(JobType.FULL_TIME);
        requestNull.setWorkMode(WorkMode.ONSITE);
        requestNull.setExpiredAt(null);

        Set<ConstraintViolation<JobRequest>> violationsNull = validator.validate(requestNull);
        assertTrue(violationsNull.isEmpty());
    }

    @Test
    @DisplayName("21. JobService.createJob ném BAD_REQUEST khi expiredAt ở quá khứ")
    void testCreateJob_ExpiredAtInPastThrowsBadRequest() {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setJobType(JobType.FULL_TIME);
        request.setWorkMode(WorkMode.ONSITE);
        request.setExpiredAt(LocalDateTime.now().minusHours(2));

        Company company = new Company();
        company.setId(1);
        company.setStatus("APPROVED");

        CompanyMember member = new CompanyMember(company, new User(), "COMPANY_ADMIN");

        when(memberRepository.findByUserId(1)).thenReturn(List.of(member));
        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.createJob(1, request));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Hạn ứng tuyển không được ở quá khứ"));
    }
}

