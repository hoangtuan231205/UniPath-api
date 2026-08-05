package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FeedItemResponse {
    private String type; // "JOB" or "POST"
    private Integer id;
    private String title;
    private String content;
    private String authorOrCompany;
    private String avatarUrl;
    private Boolean isLiked;
    private LocalDateTime timestamp;
    private JobResponse jobDetails;
    private CommunityPostResponse postDetails;
}
