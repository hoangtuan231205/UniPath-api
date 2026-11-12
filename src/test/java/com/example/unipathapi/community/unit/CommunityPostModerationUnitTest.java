package com.example.unipathapi.community.unit;

import com.example.unipathapi.admin.dto.request.ReportRequest;
import com.example.unipathapi.admin.dto.request.ReportResolveRequest;
import com.example.unipathapi.admin.dto.response.ReportResponse;
import com.example.unipathapi.admin.entity.Report;
import com.example.unipathapi.admin.repository.AdminAuditLogRepository;
import com.example.unipathapi.admin.repository.ReportRepository;
import com.example.unipathapi.admin.service.AdminAuditLogService;
import com.example.unipathapi.admin.service.AdminService;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.community.dto.request.CommentRequest;
import com.example.unipathapi.community.dto.response.CommunityPostResponse;
import com.example.unipathapi.community.entity.CommunityPost;
import com.example.unipathapi.community.repository.CommunityPostRepository;
import com.example.unipathapi.community.repository.PostCommentRepository;
import com.example.unipathapi.community.repository.PostLikeRepository;
import com.example.unipathapi.community.repository.PostMediaRepository;
import com.example.unipathapi.community.service.CommunityPostService;
import com.example.unipathapi.community.service.InteractionService;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.job.repository.JobRepository;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommunityPostModerationUnitTest {

    @Mock
    private CommunityPostRepository communityPostRepository;

    @Mock
    private CommunityPostRepository postRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private EmployerProfileRepository employerProfileRepository;

    @Mock
    private AdminAuditLogService auditLogService;

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostMediaRepository postMediaRepository;

    @InjectMocks
    private AdminService adminService;

    @InjectMocks
    private CommunityPostService postService;

    @InjectMocks
    private InteractionService interactionService;

    private User author;
    private User reporter;
    private User admin;
    private CommunityPost activePost;
    private CommunityPost inactivePost;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(10);
        author.setEmail("author@example.com");
        author.setRole("CANDIDATE");

        reporter = new User();
        reporter.setId(20);
        reporter.setEmail("reporter@example.com");
        reporter.setRole("CANDIDATE");

        admin = new User();
        admin.setId(1);
        admin.setEmail("admin@unipath.com");
        admin.setRole("ADMIN");

        activePost = new CommunityPost();
        activePost.setId(100);
        activePost.setTitle("Active Post Title");
        activePost.setContent("Active Post Content");
        activePost.setIsActive(true);
        activePost.setAuthor(author);
        activePost.setCreatedAt(LocalDateTime.now());

        inactivePost = new CommunityPost();
        inactivePost.setId(200);
        inactivePost.setTitle("Banned Post Title");
        inactivePost.setContent("Banned Post Content");
        inactivePost.setIsActive(false);
        inactivePost.setAuthor(author);
        inactivePost.setCreatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // 1. REPORT POST FLOW TESTS
    // =========================================================================

    @Test
    @DisplayName("reportPost: Thành công khi bài viết đang hoạt động và người báo cáo khác tác giả")
    void testReportPost_Success() {
        ReportRequest req = new ReportRequest();
        req.setReason("[Spam / Quảng cáo rác] Link spam lừa đảo");

        when(communityPostRepository.findById(100)).thenReturn(Optional.of(activePost));
        when(reportRepository.existsByReporterIdAndPostIdAndStatus(20, 100, "PENDING")).thenReturn(false);
        when(userRepository.findById(20)).thenReturn(Optional.of(reporter));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            r.setId(501);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        ReportResponse res = adminService.reportPost(100, 20, req);

        assertNotNull(res);
        assertEquals("POST", res.getTargetType());
        assertEquals(100, res.getPostId());
        assertEquals("PENDING", res.getStatus());
        assertEquals("[Spam / Quảng cáo rác] Link spam lừa đảo", res.getReason());
        verify(reportRepository, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("reportPost: Chặn người dùng tự báo cáo bài viết của chính mình")
    void testReportPost_SelfReportBlocked() {
        ReportRequest req = new ReportRequest();
        req.setReason("Tự báo cáo bài mình");

        when(communityPostRepository.findById(100)).thenReturn(Optional.of(activePost));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                adminService.reportPost(100, 10, req) // reporterId == author.getId()
        );

        assertTrue(ex.getMessage().contains("không thể tự báo cáo"));
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("reportPost: Chặn báo cáo bài viết đã bị khóa hoặc không còn khả dụng")
    void testReportPost_InactivePostBlocked() {
        ReportRequest req = new ReportRequest();
        req.setReason("Báo cáo bài đã khóa");

        when(communityPostRepository.findById(200)).thenReturn(Optional.of(inactivePost));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                adminService.reportPost(200, 20, req)
        );

        assertTrue(ex.getMessage().contains("đã bị khóa"));
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    @DisplayName("reportPost: Chặn báo cáo trùng lặp khi đã có report PENDING")
    void testReportPost_DuplicatePendingBlocked() {
        ReportRequest req = new ReportRequest();
        req.setReason("Spam lần 2");

        when(communityPostRepository.findById(100)).thenReturn(Optional.of(activePost));
        when(reportRepository.existsByReporterIdAndPostIdAndStatus(20, 100, "PENDING")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                adminService.reportPost(100, 20, req)
        );

        assertTrue(ex.getMessage().contains("đang chờ quản trị viên xử lý"));
        verify(reportRepository, never()).save(any(Report.class));
    }

    // =========================================================================
    // 2. ADMIN RESOLVE REPORT TESTS (BAN_POST & REJECT_REPORT)
    // =========================================================================

    @Test
    @DisplayName("resolveReport: BAN_POST khóa bài viết và batch resolve các report pending khác")
    void testResolveReport_BanPost_Success() {
        Report report1 = new Report();
        report1.setId(501);
        report1.setTargetType("POST");
        report1.setPost(activePost);
        report1.setReporter(reporter);
        report1.setStatus("PENDING");
        report1.setCreatedAt(LocalDateTime.now());

        Report report2 = new Report();
        report2.setId(502);
        report2.setTargetType("POST");
        report2.setPost(activePost);
        report2.setStatus("PENDING");
        report2.setCreatedAt(LocalDateTime.now());

        ReportResolveRequest req = new ReportResolveRequest();
        req.setAction("BAN_POST");

        when(reportRepository.findById(501)).thenReturn(Optional.of(report1));
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(reportRepository.findByPostIdAndStatus(100, "PENDING")).thenReturn(new ArrayList<>(List.of(report1, report2)));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse res = adminService.resolveReport(501, 1, req);

        assertNotNull(res);
        assertEquals("RESOLVED", res.getStatus());
        assertFalse(activePost.getIsActive()); // Post has been banned
        verify(communityPostRepository, times(1)).save(activePost);
        verify(reportRepository, times(1)).saveAll(any());
        assertEquals("RESOLVED", report2.getStatus()); // Report 2 auto-resolved
        verify(auditLogService, times(1)).log(eq(1), eq("BAN_POST"), eq("POST"), eq(100), anyString());
    }

    @Test
    @DisplayName("resolveReport: REJECT_REPORT bác bỏ report và giữ nguyên bài viết active")
    void testResolveReport_RejectReport_Success() {
        Report report = new Report();
        report.setId(501);
        report.setTargetType("POST");
        report.setPost(activePost);
        report.setReporter(reporter);
        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());

        ReportResolveRequest req = new ReportResolveRequest();
        req.setAction("REJECT_REPORT");

        when(reportRepository.findById(501)).thenReturn(Optional.of(report));
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponse res = adminService.resolveReport(501, 1, req);

        assertNotNull(res);
        assertEquals("REJECTED", res.getStatus());
        assertTrue(activePost.getIsActive()); // Post remains active
        verify(communityPostRepository, never()).save(any());
        verify(auditLogService, times(1)).log(eq(1), eq("REJECT_REPORT"), eq("REPORT"), eq(501), anyString());
    }

    @Test
    @DisplayName("resolveReport: Từ chối action không hợp lệ như BAN_ACCOUNT cho POST")
    void testResolveReport_InvalidActionForPost_ThrowsException() {
        Report report = new Report();
        report.setId(501);
        report.setTargetType("POST");
        report.setPost(activePost);
        report.setStatus("PENDING");

        ReportResolveRequest req = new ReportResolveRequest();
        req.setAction("BAN_ACCOUNT"); // Invalid for POST report

        when(reportRepository.findById(501)).thenReturn(Optional.of(report));
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                adminService.resolveReport(501, 1, req)
        );

        assertTrue(ex.getMessage().contains("Chỉ chấp nhận BAN_POST hoặc REJECT_REPORT"));
    }

    @Test
    @DisplayName("resolveReport: Chặn xử lý lại report đã ở trạng thái terminal (không phải PENDING)")
    void testResolveReport_NonPendingStatus_ThrowsException() {
        Report report = new Report();
        report.setId(501);
        report.setTargetType("POST");
        report.setStatus("RESOLVED");

        ReportResolveRequest req = new ReportResolveRequest();
        req.setAction("BAN_POST");

        when(reportRepository.findById(501)).thenReturn(Optional.of(report));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                adminService.resolveReport(501, 1, req)
        );

        assertTrue(ex.getMessage().contains("không thể xử lý lại"));
    }

    // =========================================================================
    // 3. ACCESS CONTROL ON INACTIVE POSTS TESTS
    // =========================================================================

    @Test
    @DisplayName("getPostDetail: Khách vãng lai (role == null) không được xem bài viết bị khóa")
    void testGetPostDetail_AnonymousBlockedOnInactivePost() {
        when(postRepository.findById(200)).thenReturn(Optional.of(inactivePost));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                postService.getPostDetail(200, null, null)
        );

        assertTrue(ex.getMessage().contains("bị khóa do vi phạm"));
    }

    @Test
    @DisplayName("getPostDetail: Người dùng CANDIDATE thông thường không được xem bài viết bị khóa")
    void testGetPostDetail_CandidateBlockedOnInactivePost() {
        when(postRepository.findById(200)).thenReturn(Optional.of(inactivePost));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                postService.getPostDetail(200, 20, "CANDIDATE")
        );

        assertTrue(ex.getMessage().contains("bị khóa do vi phạm"));
    }

    @Test
    @DisplayName("getPostDetail: Quản trị viên (role == 'ADMIN') ĐƯỢC PHÉP xem bài viết bị khóa")
    void testGetPostDetail_AdminAllowedOnInactivePost() {
        when(postRepository.findById(200)).thenReturn(Optional.of(inactivePost));
        when(postLikeRepository.existsByPostIdAndUserId(200, 1)).thenReturn(false);
        when(postMediaRepository.findByPostIdOrderByDisplayOrderAsc(200)).thenReturn(Collections.emptyList());

        CommunityPostResponse res = postService.getPostDetail(200, 1, "ADMIN");

        assertNotNull(res);
        assertEquals(200, res.getId());
        assertFalse(res.getIsActive()); // isActive is false
    }

    @Test
    @DisplayName("uploadPostMedia: Chặn đính kèm tệp trên bài viết đã bị khóa")
    void testUploadPostMedia_InactivePostBlocked() {
        when(postRepository.findById(200)).thenReturn(Optional.of(inactivePost));

        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[10]);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                postService.uploadPostMedia(200, 10, List.of(file))
        );

        assertTrue(ex.getMessage().contains("đã bị khóa vi phạm"));
    }

    // =========================================================================
    // 4. INTERACTION SERVICE TESTS ON INACTIVE POSTS
    // =========================================================================

    @Test
    @DisplayName("likePost: Chặn tương tác like trên bài viết bị khóa")
    void testLikePost_InactivePostBlocked() {
        when(postRepository.findById(200)).thenReturn(Optional.of(inactivePost));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                interactionService.likePost(200, 20)
        );

        assertTrue(ex.getMessage().contains("Bài viết đã bị khóa"));
    }

    @Test
    @DisplayName("createPostComment: Chặn bình luận trên bài viết bị khóa")
    void testCreatePostComment_InactivePostBlocked() {
        CommentRequest req = new CommentRequest();
        req.setContent("Bình luận vào bài vi phạm");

        when(postRepository.findById(200)).thenReturn(Optional.of(inactivePost));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                interactionService.createPostComment(200, 20, req)
        );

        assertTrue(ex.getMessage().contains("Bài viết đã bị khóa"));
    }
}
