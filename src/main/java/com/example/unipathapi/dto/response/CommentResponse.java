package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CommentResponse {
    private Integer id;
    private Integer jobId;
    private Integer postId;
    private Integer userId;
    private String userName;
    private Integer parentCommentId;
    private String content;
    private LocalDateTime createdAt;
}
