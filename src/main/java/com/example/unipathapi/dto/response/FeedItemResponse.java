package com.example.unipathapi.dto.response;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import java.util.List;

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
    private List<PostMediaResponse> media;
}
