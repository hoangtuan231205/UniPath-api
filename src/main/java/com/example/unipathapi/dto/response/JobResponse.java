package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class JobResponse {
    private Integer id;
    private String title;
    private String jobType;
    private String salaryRange;
    private String description;
    private String requirements;

    private Integer categoryId;
    private String categoryName;

    private Integer locationId;
    private String locationAddress;

    private Integer companyId;
    private String companyName;
    private String companyScale;
    private String companyLogoUrl;

    private List<String> skills;

    private long likesCount;
    private long commentsCount;
    private long sharesCount;
    private long applicationsCount;
    private Boolean isLiked;

    private Boolean isActive;
    private LocalDateTime postedAt;
    private LocalDateTime expiredAt;

    private String type; // Always "JOB" for feed disambiguation
}
