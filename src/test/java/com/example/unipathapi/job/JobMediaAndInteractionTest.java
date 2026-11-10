package com.example.unipathapi.job;

import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.application.service.ApplicationService;
import com.example.unipathapi.common.storage.FileStorageService;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyMember;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.job.dto.response.JobApplicationStatsDto;
import com.example.unipathapi.job.dto.response.JobMediaResponse;
import com.example.unipathapi.job.dto.response.JobResponse;
import com.example.unipathapi.job.entity.Job;
import com.example.unipathapi.job.entity.JobMedia;
import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.service.JobService;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobMediaAndInteractionTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobMediaRepository jobMediaRepository;

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobLikeRepository jobLikeRepository;

    @Mock
    private JobCommentRepository jobCommentRepository;

    @Mock
    private JobShareRepository jobShareRepository;

    @Mock
    private JobSkillRepository jobSkillRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private JobService jobService;

    @InjectMocks
    private ApplicationService applicationService;

    private User mockCandidate;
    private User mockEmployer;
    private Company mockCompany;
    private CompanyMember mockMember;
    private Job mockJob;

    @BeforeEach
    void setUp() {
        mockCandidate = new User();
        mockCandidate.setId(101);
        mockCandidate.setRole("CANDIDATE");

        mockEmployer = new User();
        mockEmployer.setId(202);
        mockEmployer.setRole("EMPLOYER");

        mockCompany = new Company();
        mockCompany.setId(10);
        mockCompany.setCompanyName("UniPath Tech");
        mockCompany.setStatus("APPROVED");

        mockMember = new CompanyMember();
        mockMember.setId(1);
        mockMember.setUser(mockEmployer);
        mockMember.setCompany(mockCompany);
        mockMember.setMemberRole("COMPANY_ADMIN");

        mockJob = new Job();
        mockJob.setId(17);
        mockJob.setTitle("Backend Engineer");
        mockJob.setCompany(mockCompany);
        mockJob.setJobType(JobType.FULL_TIME);
        mockJob.setWorkMode(WorkMode.ONSITE);
        mockJob.setIsActive(true);
        mockJob.setPostedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("1. Batch Feed: Không bị lỗi N+1, tất cả sub-resource chỉ query đúng 1 lần cho cả batch")
    void testBatchFeed_ZeroNPlusOneQueries() {
        Job job1 = new Job();
        job1.setId(1);
        job1.setTitle("Job 1");
        job1.setCompany(mockCompany);
        job1.setJobType(JobType.FULL_TIME);
        job1.setWorkMode(WorkMode.ONSITE);
        job1.setIsActive(true);

        Job job2 = new Job();
        job2.setId(2);
        job2.setTitle("Job 2");
        job2.setCompany(mockCompany);
        job2.setJobType(JobType.INTERNSHIP);
        job2.setWorkMode(WorkMode.HYBRID);
        job2.setIsActive(true);

        when(jobRepository.findFeedJobs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(job1, job2));
        when(userRepository.findById(mockCandidate.getId())).thenReturn(Optional.of(mockCandidate));
        when(jobLikeRepository.findLikedJobIdsByUserIdAndJobIds(eq(mockCandidate.getId()), anyCollection()))
                .thenReturn(Set.of(1));
        when(savedJobRepository.findSavedJobIdsByCandidateIdAndJobIds(eq(mockCandidate.getId()), anyCollection()))
                .thenReturn(Set.of(2));
        when(applicationRepository.findAppliedJobIdsByCandidateIdAndJobIds(eq(mockCandidate.getId()), anyCollection()))
                .thenReturn(Set.of(1));

        when(jobLikeRepository.countLikesByJobIds(anyCollection())).thenReturn(Collections.emptyList());
        when(jobCommentRepository.countCommentsByJobIds(anyCollection())).thenReturn(Collections.emptyList());
        when(applicationRepository.getStatsForJobIds(anyCollection())).thenReturn(Collections.emptyList());
        when(jobSkillRepository.findByJobIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(jobMediaRepository.findByJobIdIn(anyCollection())).thenReturn(Collections.emptyList());

        List<JobResponse> responses = jobService.getFeedJobs(null, null, null, null, null, null, mockCandidate.getId());

        assertEquals(2, responses.size());
        assertTrue(responses.get(0).isLiked());
        assertTrue(responses.get(0).isApplied());
        assertFalse(responses.get(0).isSaved());

        assertFalse(responses.get(1).isLiked());
        assertFalse(responses.get(1).isApplied());
        assertTrue(responses.get(1).isSaved());

        // Xác nhận không bị N+1: mỗi repository chỉ được gọi đúng 1 lần cho toàn bộ batch
        verify(jobRepository, times(1)).findFeedJobs(any(), any(), any(), any(), any(), any(), any());
        verify(jobLikeRepository, times(1)).countLikesByJobIds(anyCollection());
        verify(jobCommentRepository, times(1)).countCommentsByJobIds(anyCollection());
        verify(applicationRepository, times(1)).getStatsForJobIds(anyCollection());
        verify(jobSkillRepository, times(1)).findByJobIdIn(anyCollection());
        verify(jobMediaRepository, times(1)).findByJobIdIn(anyCollection());
        verify(jobLikeRepository, times(1)).findLikedJobIdsByUserIdAndJobIds(any(), anyCollection());
        verify(savedJobRepository, times(1)).findSavedJobIdsByCandidateIdAndJobIds(any(), anyCollection());
        verify(applicationRepository, times(1)).findAppliedJobIdsByCandidateIdAndJobIds(any(), anyCollection());
    }

    @Test
    @DisplayName("2. Chi tiết Job: Trả về đầy đủ media theo thứ tự displayOrder và coverMedia là phần tử đầu tiên")
    void testGetJobDetail_WithMedia_OrderedCorrectly() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));

        JobMedia m0 = new JobMedia();
        m0.setId(1);
        m0.setJob(mockJob);
        m0.setFileUrl("/uploads/jobs/17/img1.jpg");
        m0.setFileType("IMAGE");
        m0.setDisplayOrder(0);

        JobMedia m1 = new JobMedia();
        m1.setId(2);
        m1.setJob(mockJob);
        m1.setFileUrl("/uploads/jobs/17/video1.mp4");
        m1.setFileType("VIDEO");
        m1.setDisplayOrder(1);

        when(jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(17)).thenReturn(List.of(m0, m1));
        when(jobSkillRepository.findByJobId(17)).thenReturn(Collections.emptyList());

        JobResponse res = jobService.getJobDetail(17, null);

        assertNotNull(res);
        assertNotNull(res.getCoverMedia());
        assertEquals("/uploads/jobs/17/img1.jpg", res.getCoverMedia().getFileUrl());
        assertEquals(2, res.getMedia().size());
        assertEquals(0, res.getMedia().get(0).getDisplayOrder());
        assertEquals(1, res.getMedia().get(1).getDisplayOrder());
    }

    @Test
    @DisplayName("3. Chi tiết Job không có media: coverMedia là null và media là mảng rỗng (không khung ảnh trống)")
    void testGetJobDetail_NoMedia() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(17)).thenReturn(Collections.emptyList());
        when(jobSkillRepository.findByJobId(17)).thenReturn(Collections.emptyList());

        JobResponse res = jobService.getJobDetail(17, null);

        assertNotNull(res);
        assertNull(res.getCoverMedia());
        assertNotNull(res.getMedia());
        assertTrue(res.getMedia().isEmpty());
    }

    @Test
    @DisplayName("4. Candidate: saved=true và applied=true khi đã lưu và đã nộp hồ sơ")
    void testCandidate_SavedAndApplied_True() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(userRepository.findById(mockCandidate.getId())).thenReturn(Optional.of(mockCandidate));
        when(jobLikeRepository.existsByJobIdAndUserId(17, mockCandidate.getId())).thenReturn(true);
        when(savedJobRepository.existsByCandidateIdAndJobId(mockCandidate.getId(), 17)).thenReturn(true);
        when(applicationRepository.existsByJobIdAndCandidateId(17, mockCandidate.getId())).thenReturn(true);

        JobResponse res = jobService.getJobDetail(17, mockCandidate.getId());

        assertTrue(res.isLiked());
        assertTrue(res.isSaved());
        assertTrue(res.isApplied());
    }

    @Test
    @DisplayName("5. Employer: luôn nhận saved=false và applied=false (Chế độ tham khảo thị trường)")
    void testEmployer_SavedAndApplied_AlwaysFalse() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(userRepository.findById(mockEmployer.getId())).thenReturn(Optional.of(mockEmployer));

        JobResponse res = jobService.getJobDetail(17, mockEmployer.getId());

        assertFalse(res.isSaved());
        assertFalse(res.isApplied());
        assertFalse(res.isLiked());
    }

    @Test
    @DisplayName("6. Khách vãng lai chưa đăng nhập: luôn nhận saved=false và applied=false")
    void testGuest_SavedAndApplied_AlwaysFalse() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));

        JobResponse res = jobService.getJobDetail(17, null);

        assertFalse(res.isSaved());
        assertFalse(res.isApplied());
        assertFalse(res.isLiked());
    }

    @Test
    @DisplayName("7. Upload Media thành công: Employer thuộc công ty upload 1 file ảnh hợp lệ")
    void testUploadJobMedia_Success() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(memberRepository.findByCompanyIdAndUserId(10, mockEmployer.getId())).thenReturn(Optional.of(mockMember));
        when(jobMediaRepository.countByJobId(17)).thenReturn(0L);

        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "office.jpg", "image/jpeg", jpegHeader);

        when(fileStorageService.storeSingleJobMedia(any(), eq("jobs/17"))).thenReturn("/uploads/jobs/17/uuid.jpg");

        JobMedia savedMedia = new JobMedia();
        savedMedia.setId(99);
        savedMedia.setJob(mockJob);
        savedMedia.setFileUrl("/uploads/jobs/17/uuid.jpg");
        savedMedia.setFileType("IMAGE");
        savedMedia.setDisplayOrder(0);
        when(jobMediaRepository.save(any(JobMedia.class))).thenReturn(savedMedia);

        JobMediaResponse response = jobService.uploadJobMedia(17, mockEmployer.getId(), file);

        assertNotNull(response);
        assertEquals("/uploads/jobs/17/uuid.jpg", response.getFileUrl());
        assertEquals("IMAGE", response.getFileType());
        assertEquals(0, response.getDisplayOrder());
    }

    @Test
    @DisplayName("8. Upload Media thất bại: Employer không thuộc công ty bị từ chối")
    void testUploadJobMedia_UnauthorizedCompany() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(memberRepository.findByCompanyIdAndUserId(10, 999)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2});

        assertThrows(RuntimeException.class, () -> jobService.uploadJobMedia(17, 999, file));
    }

    @Test
    @DisplayName("9. Upload Media thất bại: Vượt quá giới hạn 6 media cho một Job")
    void testUploadJobMedia_ExceedMaxCount() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(memberRepository.findByCompanyIdAndUserId(10, mockEmployer.getId())).thenReturn(Optional.of(mockMember));
        when(jobMediaRepository.countByJobId(17)).thenReturn(6L);

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2});

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.uploadJobMedia(17, mockEmployer.getId(), file));
        assertTrue(ex.getReason().contains("tối đa 6 tệp"));
    }

    @Test
    @DisplayName("10. Rollback file vật lý nếu lưu DB gặp lỗi")
    void testUploadJobMedia_RollbackFileOnDbError() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(memberRepository.findByCompanyIdAndUserId(10, mockEmployer.getId())).thenReturn(Optional.of(mockMember));
        when(jobMediaRepository.countByJobId(17)).thenReturn(1L);

        MockMultipartFile file = new MockMultipartFile("file", "office.png", "image/png", new byte[]{1, 2, 3});
        when(fileStorageService.storeSingleJobMedia(any(), eq("jobs/17"))).thenReturn("/uploads/jobs/17/error_test.png");
        when(jobMediaRepository.save(any())).thenThrow(new RuntimeException("DB Connection Timeout"));

        assertThrows(ResponseStatusException.class, () -> jobService.uploadJobMedia(17, mockEmployer.getId(), file));

        // Xác nhận file vừa ghi phải bị xóa ngay lập tức để không để lại file rác
        verify(fileStorageService, times(1)).deleteFile("/uploads/jobs/17/error_test.png");
    }

    @Test
    @DisplayName("11. Xóa Job: Từ chối xóa nếu đã có ứng viên nộp hồ sơ")
    void testDeleteJob_PreventIfHasApplications() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(memberRepository.findByCompanyIdAndUserId(10, mockEmployer.getId())).thenReturn(Optional.of(mockMember));
        when(applicationRepository.countByJobId(17)).thenReturn(3L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> jobService.deleteJob(17, mockEmployer.getId()));
        assertTrue(ex.getReason().contains("đã có người ứng tuyển"));
    }

    @Test
    @DisplayName("12. Xóa Job: Khi chưa có ứng viên, xóa toàn bộ JobMedia và file vật lý trên đĩa")
    void testDeleteJob_DeletesMediaAndPhysicalFiles() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(memberRepository.findByCompanyIdAndUserId(10, mockEmployer.getId())).thenReturn(Optional.of(mockMember));
        when(applicationRepository.countByJobId(17)).thenReturn(0L);

        JobMedia m1 = new JobMedia();
        m1.setId(10);
        m1.setFileUrl("/uploads/jobs/17/pic1.jpg");
        when(jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(17)).thenReturn(List.of(m1));

        jobService.deleteJob(17, mockEmployer.getId());

        verify(fileStorageService, times(1)).deleteFile("/uploads/jobs/17/pic1.jpg");
        verify(jobMediaRepository, times(1)).deleteByJobId(17);
        verify(jobSkillRepository, times(1)).deleteByJobId(17);
        verify(jobRepository, times(1)).delete(mockJob);
    }

    @Test
    @DisplayName("13. Sắp xếp thứ tự Media (updateMediaOrder): Cập nhật displayOrder nối tiếp")
    void testUpdateMediaOrder_Success() {
        when(jobRepository.findById(17)).thenReturn(Optional.of(mockJob));
        when(memberRepository.findByCompanyIdAndUserId(10, mockEmployer.getId())).thenReturn(Optional.of(mockMember));

        JobMedia m1 = new JobMedia();
        m1.setId(10);
        m1.setDisplayOrder(0);
        JobMedia m2 = new JobMedia();
        m2.setId(20);
        m2.setDisplayOrder(1);

        when(jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(17)).thenReturn(List.of(m1, m2));

        jobService.updateMediaOrder(17, mockEmployer.getId(), List.of(20, 10));

        assertEquals(1, m1.getDisplayOrder());
        assertEquals(0, m2.getDisplayOrder());
        verify(jobMediaRepository, times(2)).save(any(JobMedia.class));
    }
}
