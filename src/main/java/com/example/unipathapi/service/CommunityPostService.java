package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CommunityPostRequest;
import com.example.unipathapi.dto.response.CommunityPostResponse;
import com.example.unipathapi.dto.response.FeedItemResponse;
import com.example.unipathapi.dto.response.JobResponse;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        return buildPostResponse(savedPost, false);
    }

    public CommunityPostResponse getPostDetail(Integer id) {
        return getPostDetail(id, null);
    }

    public CommunityPostResponse getPostDetail(Integer id, Integer currentUserId) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết cộng đồng"));
        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = postLikeRepository.existsByPostIdAndUserId(id, currentUserId);
        }
        return buildPostResponse(post, isLiked);
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
        return buildPostResponse(updatedPost, false);
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
        return getMergedFeed(cursor, null);
    }

    public List<FeedItemResponse> getMergedFeed(Integer cursor, Integer currentUserId) {
        Pageable pageable = PageRequest.of(0, 20);

        List<JobResponse> jobs = jobService.getFeedJobs(cursor, null, null, null, null, currentUserId);
        List<CommunityPost> posts = postRepository.findPostsFeed(cursor, pageable);

        Set<Integer> likedPostIds;
        if (currentUserId != null && !posts.isEmpty()) {
            List<Integer> postIds = posts.stream().map(CommunityPost::getId).collect(Collectors.toList());
            likedPostIds = postLikeRepository.findLikedPostIdsByUserIdAndPostIds(currentUserId, postIds);
        } else {
            likedPostIds = Collections.emptySet();
        }

        List<FeedItemResponse> feedItems = new ArrayList<>();

        for (JobResponse job : jobs) {
            feedItems.add(FeedItemResponse.builder()
                    .type("JOB")
                    .id(job.getId())
                    .title(job.getTitle())
                    .content(job.getDescription())
                    .authorOrCompany(job.getCompanyName())
                    .avatarUrl(job.getCompanyLogoUrl())
                    .isLiked(job.getIsLiked())
                    .timestamp(job.getPostedAt())
                    .jobDetails(job)
                    .build());
        }

        for (CommunityPost post : posts) {
            boolean isLiked = likedPostIds.contains(post.getId());
            CommunityPostResponse postResp = buildPostResponse(post, isLiked);
            feedItems.add(FeedItemResponse.builder()
                    .type("POST")
                    .id(postResp.getId())
                    .title(postResp.getTitle())
                    .content(postResp.getContent())
                    .authorOrCompany(postResp.getAuthorName())
                    .avatarUrl(postResp.getAuthorAvatarUrl())
                    .isLiked(postResp.getIsLiked())
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
        return buildPostResponse(post, false);
    }

    public CommunityPostResponse buildPostResponse(CommunityPost post, Boolean isLiked) {
        String authorName = "User #" + post.getAuthor().getId();
        String authorAvatarUrl = null;

        if ("CANDIDATE".equalsIgnoreCase(post.getAuthor().getRole())) {
            Optional<CandidateProfile> profileOpt = candidateProfileRepository.findById(post.getAuthor().getId());
            authorName = profileOpt.map(CandidateProfile::getFullName).orElse(post.getAuthor().getEmail());
            authorAvatarUrl = profileOpt.map(CandidateProfile::getAvatarUrl).orElse(null);
        } else if ("EMPLOYER".equalsIgnoreCase(post.getAuthor().getRole())) {
            Optional<EmployerProfile> profileOpt = employerProfileRepository.findById(post.getAuthor().getId());
            authorName = profileOpt.map(EmployerProfile::getFullName).orElse(post.getAuthor().getEmail());
            authorAvatarUrl = profileOpt.map(EmployerProfile::getAvatarUrl).orElse(null);
        }

        long likesCount = postLikeRepository.countByPostId(post.getId());
        long commentsCount = postCommentRepository.countByPostId(post.getId());

        return CommunityPostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(authorName)
                .authorAvatarUrl(authorAvatarUrl)
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .likesCount(likesCount)
                .commentsCount(commentsCount)
                .isLiked(isLiked != null ? isLiked : false)
                .type("POST")
                .build();
    }
}
