package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CommunityPostResponse {
    private Integer id;
    private Integer authorId;
    private String authorName;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private long likesCount;
    private long commentsCount;
    private String type; // Always "POST"
}
