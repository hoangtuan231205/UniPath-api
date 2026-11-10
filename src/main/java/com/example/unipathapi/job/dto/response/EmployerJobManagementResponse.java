package com.example.unipathapi.job.dto.response;

import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployerJobManagementResponse {
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
    private String companyLogoUrl;

    private List<String> skills;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime postedAt;
    private LocalDateTime expiredAt;

    // Specific metrics for employer management card
    private long applicationsCount;
    private long newApplicationsCount;
    private long interviewApplicationsCount;

    // Computed effective status: "ACTIVE", "CLOSED", "EXPIRED"
    private String effectiveStatus;
}
