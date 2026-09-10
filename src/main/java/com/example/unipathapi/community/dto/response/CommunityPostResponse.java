package com.example.unipathapi.community.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import java.util.List;

@Getter
@Setter
@Builder
public class CommunityPostResponse {
    private Integer id;
    private Integer authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private long likesCount;
    private long commentsCount;
    private Boolean isLiked;
    private String type; // Always "POST"
    private List<PostMediaResponse> media;
}
