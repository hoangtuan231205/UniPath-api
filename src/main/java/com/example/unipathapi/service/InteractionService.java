package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CommentRequest;
import com.example.unipathapi.dto.response.CommentResponse;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private JobLikeRepository jobLikeRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private JobCommentRepository jobCommentRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private JobShareRepository jobShareRepository;

    // --- JOB LIKE ---
    public void likeJob(Integer jobId, Integer userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!jobLikeRepository.existsByJobIdAndUserId(jobId, userId)) {
            JobLike like = new JobLike();
            like.setJob(job);
            like.setUser(user);
            jobLikeRepository.save(like);
        }
    }

    public void unlikeJob(Integer jobId, Integer userId) {
        Optional<JobLike> like = jobLikeRepository.findByJobIdAndUserId(jobId, userId);
        like.ifPresent(jobLikeRepository::delete);
    }

    // --- POST LIKE ---
    public void likePost(Integer postId, Integer userId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            PostLike like = new PostLike();
            like.setPost(post);
            like.setUser(user);
            postLikeRepository.save(like);
        }
    }

    public void unlikePost(Integer postId, Integer userId) {
        Optional<PostLike> like = postLikeRepository.findByPostIdAndUserId(postId, userId);
        like.ifPresent(postLikeRepository::delete);
    }

    // --- JOB COMMENTS ---
    public List<CommentResponse> getJobComments(Integer jobId, Integer cursor) {
        Pageable pageable = PageRequest.of(0, 20);
        List<JobComment> comments = jobCommentRepository.findCommentsByJobId(jobId, cursor, pageable);
        return comments.stream().map(this::buildJobCommentResponse).collect(Collectors.toList());
    }

    public CommentResponse createJobComment(Integer jobId, Integer userId, CommentRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        JobComment comment = new JobComment();
        comment.setJob(job);
        comment.setUser(user);
        comment.setContent(request.getContent());

        if (request.getParentCommentId() != null) {
            JobComment parent = jobCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Bình luận cha không tồn tại"));
            comment.setParentComment(parent);
        }

        JobComment saved = jobCommentRepository.save(comment);
        return buildJobCommentResponse(saved);
    }

    public void deleteJobComment(Integer commentId, Integer userId) {
        JobComment comment = jobCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xoá bình luận này");
        }

        jobCommentRepository.delete(comment);
    }

    // --- POST COMMENTS ---
    public List<CommentResponse> getPostComments(Integer postId, Integer cursor) {
        Pageable pageable = PageRequest.of(0, 20);
        List<PostComment> comments = postCommentRepository.findCommentsByPostId(postId, cursor, pageable);
        return comments.stream().map(this::buildPostCommentResponse).collect(Collectors.toList());
    }

    public CommentResponse createPostComment(Integer postId, Integer userId, CommentRequest request) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(request.getContent());

        PostComment saved = postCommentRepository.save(comment);
        return buildPostCommentResponse(saved);
    }

    public void deletePostComment(Integer commentId, Integer userId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xoá bình luận này");
        }

        postCommentRepository.delete(comment);
    }

    // --- JOB SHARE ---
    public void shareJob(Integer jobId, Integer userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        JobShare share = new JobShare();
        share.setJob(job);
        share.setUser(user);
        jobShareRepository.save(share);
    }

    // --- HELPER METHODS ---
    private String getUserName(User user) {
        if ("CANDIDATE".equalsIgnoreCase(user.getRole())) {
            return candidateProfileRepository.findById(user.getId())
                    .map(CandidateProfile::getFullName).orElse(user.getEmail());
        } else if ("EMPLOYER".equalsIgnoreCase(user.getRole())) {
            return employerProfileRepository.findById(user.getId())
                    .map(EmployerProfile::getFullName).orElse(user.getEmail());
        }
        return user.getEmail();
    }

    private CommentResponse buildJobCommentResponse(JobComment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .jobId(c.getJob().getId())
                .userId(c.getUser().getId())
                .userName(getUserName(c.getUser()))
                .parentCommentId(c.getParentComment() != null ? c.getParentComment().getId() : null)
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private CommentResponse buildPostCommentResponse(PostComment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .postId(c.getPost().getId())
                .userId(c.getUser().getId())
                .userName(getUserName(c.getUser()))
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
