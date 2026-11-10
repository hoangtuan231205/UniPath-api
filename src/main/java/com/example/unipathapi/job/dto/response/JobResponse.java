package com.example.unipathapi.job.dto.response;

import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Integer id;
    private String title;
    private JobType jobType;
    private WorkMode workMode;
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

    @Builder.Default
    private List<String> skills = new ArrayList<>();

    private long likesCount;
    private long commentsCount;
    private long sharesCount;
    private long applicationsCount;

    @JsonProperty("liked")
    private boolean liked;

    @JsonProperty("saved")
    private boolean saved;

    @JsonProperty("applied")
    private boolean applied;

    @JsonProperty("isLiked")
    public boolean getIsLiked() {
        return liked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.liked = (isLiked != null && isLiked);
    }

    private Boolean isActive;
    private LocalDateTime postedAt;
    private LocalDateTime expiredAt;

    private String type; // Always "JOB" for feed disambiguation

    private JobMediaResponse coverMedia;

    @Builder.Default
    private List<JobMediaResponse> media = new ArrayList<>();
}
