package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CommunityPostRequest;
import com.example.unipathapi.dto.response.CommunityPostResponse;
import com.example.unipathapi.dto.response.FeedItemResponse;
import com.example.unipathapi.dto.response.JobResponse;
import com.example.unipathapi.entity.CommunityPost;
import com.example.unipathapi.entity.User;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityPostService {

    @Autowired
    private CommunityPostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private JobService jobService;

    public CommunityPostResponse createPost(Integer userId, CommunityPostRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        CommunityPost post = new CommunityPost();
        post.setAuthor(author);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        CommunityPost savedPost = postRepository.save(post);
        return buildPostResponse(savedPost);
    }

    public CommunityPostResponse getPostDetail(Integer id) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết cộng đồng"));
        return buildPostResponse(post);
    }

    public CommunityPostResponse updatePost(Integer id, Integer userId, CommunityPostRequest request) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết cộng đồng"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không phải tác giả của bài viết này");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        CommunityPost updatedPost = postRepository.save(post);
        return buildPostResponse(updatedPost);
    }

    public void deletePost(Integer id, Integer userId) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết cộng đồng"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không phải tác giả của bài viết này");
        }

        postRepository.delete(post);
    }

    public List<FeedItemResponse> getMergedFeed(Integer cursor) {
        Pageable pageable = PageRequest.of(0, 20);

        List<JobResponse> jobs = jobService.getFeedJobs(cursor, null, null, null, null);
        List<CommunityPost> posts = postRepository.findPostsFeed(cursor, pageable);

        List<FeedItemResponse> feedItems = new ArrayList<>();

        for (JobResponse job : jobs) {
            feedItems.add(FeedItemResponse.builder()
                    .type("JOB")
                    .id(job.getId())
                    .title(job.getTitle())
                    .content(job.getDescription())
                    .authorOrCompany(job.getCompanyName())
                    .timestamp(job.getPostedAt())
                    .jobDetails(job)
                    .build());
        }

        for (CommunityPost post : posts) {
            CommunityPostResponse postResp = buildPostResponse(post);
            feedItems.add(FeedItemResponse.builder()
                    .type("POST")
                    .id(postResp.getId())
                    .title(postResp.getTitle())
                    .content(postResp.getContent())
                    .authorOrCompany(postResp.getAuthorName())
                    .timestamp(postResp.getCreatedAt())
                    .postDetails(postResp)
                    .build());
        }

        return feedItems.stream()
                .sorted((a, b) -> {
                    if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                })
                .limit(20)
                .collect(Collectors.toList());
    }

    public CommunityPostResponse buildPostResponse(CommunityPost post) {
        String authorName = "User #" + post.getAuthor().getId();
        if ("CANDIDATE".equalsIgnoreCase(post.getAuthor().getRole())) {
            authorName = candidateProfileRepository.findById(post.getAuthor().getId())
                    .map(p -> p.getFullName()).orElse(post.getAuthor().getEmail());
        } else if ("EMPLOYER".equalsIgnoreCase(post.getAuthor().getRole())) {
            authorName = employerProfileRepository.findById(post.getAuthor().getId())
                    .map(p -> p.getFullName()).orElse(post.getAuthor().getEmail());
        }

        long likesCount = postLikeRepository.countByPostId(post.getId());
        long commentsCount = postCommentRepository.countByPostId(post.getId());

        return CommunityPostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(authorName)
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .likesCount(likesCount)
                .commentsCount(commentsCount)
                .type("POST")
                .build();
    }
}
