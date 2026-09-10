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

import com.example.unipathapi.dto.response.PostMediaResponse;
import com.example.unipathapi.common.storage.FileStorageService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private PostMediaRepository postMediaRepository;

    @Autowired
    private FileStorageService fileStorageService;

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

    public List<CommunityPostResponse> getUserPosts(Integer targetUserId, Integer cursor, Integer currentUserId) {
        Pageable pageable = PageRequest.of(0, 20);
        List<CommunityPost> posts = postRepository.findUserPostsFeed(targetUserId, cursor, pageable);
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> likedPostIds;
        if (currentUserId != null) {
            List<Integer> postIds = posts.stream().map(CommunityPost::getId).collect(Collectors.toList());
            likedPostIds = postLikeRepository.findLikedPostIdsByUserIdAndPostIds(currentUserId, postIds);
        } else {
            likedPostIds = Collections.emptySet();
        }

        return posts.stream()
                .map(post -> buildPostResponse(post, likedPostIds.contains(post.getId())))
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

        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByDisplayOrderAsc(post.getId());
        List<PostMediaResponse> mediaResponses = mediaList.stream()
                .map(m -> PostMediaResponse.builder()
                        .id(m.getId())
                        .postId(post.getId())
                        .fileUrl(m.getFileUrl())
                        .fileType(m.getFileType())
                        .mimeType(m.getMimeType())
                        .fileSizeBytes(m.getFileSizeBytes())
                        .displayOrder(m.getDisplayOrder())
                        .uploadedAt(m.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        java.time.LocalDateTime createdAt = post.getCreatedAt() != null ? post.getCreatedAt() : java.time.LocalDateTime.now();

        return CommunityPostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(authorName)
                .authorAvatarUrl(authorAvatarUrl)
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(createdAt)
                .likesCount(likesCount)
                .commentsCount(commentsCount)
                .isLiked(isLiked != null ? isLiked : false)
                .type("POST")
                .media(mediaResponses)
                .build();
    }

    @Transactional
    public List<PostMediaResponse> uploadPostMedia(Integer postId, Integer userId, List<MultipartFile> files) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết cộng đồng"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không phải tác giả của bài viết này");
        }

        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> storedUrls = fileStorageService.storeFiles(files, "posts/" + postId);
        long existingCount = postMediaRepository.countByPostId(postId);

        List<PostMediaResponse> result = new ArrayList<>();
        int orderOffset = (int) existingCount;

        for (int i = 0; i < files.size() && i < storedUrls.size(); i++) {
            MultipartFile file = files.get(i);
            String url = storedUrls.get(i);
            String mimeType = file.getContentType();
            String fileType = (mimeType != null && mimeType.toLowerCase().startsWith("image/")) ? "IMAGE" : "FILE";

            PostMedia media = new PostMedia();
            media.setPost(post);
            media.setFileUrl(url);
            media.setFileType(fileType);
            media.setMimeType(mimeType);
            media.setFileSizeBytes((int) file.getSize());
            media.setDisplayOrder(orderOffset + i);

            PostMedia saved = postMediaRepository.save(media);

            result.add(PostMediaResponse.builder()
                    .id(saved.getId())
                    .postId(postId)
                    .fileUrl(saved.getFileUrl())
                    .fileType(saved.getFileType())
                    .mimeType(saved.getMimeType())
                    .fileSizeBytes(saved.getFileSizeBytes())
                    .displayOrder(saved.getDisplayOrder())
                    .uploadedAt(saved.getUploadedAt())
                    .build());
        }

        return result;
    }

    @Transactional
    public void deletePostMedia(Integer mediaId, Integer userId) {
        PostMedia media = postMediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tệp đính kèm"));

        if (!media.getPost().getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xoá tệp đính kèm này");
        }

        postMediaRepository.delete(media);
        fileStorageService.deleteFile(media.getFileUrl());
    }
}
