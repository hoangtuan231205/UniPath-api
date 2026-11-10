package com.example.unipathapi.company.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MyCompanyOverviewResponse {

    private boolean hasCompany;
    private Boolean requiresCompanySelection;
    private List<UserCompanyMembershipResponse> availableCompanies;

    // Chi tiết công ty
    private Integer companyId;
    private String companyName;
    private String companyScale;
    private String description;
    private String website;
    private String phoneNumber;
    private String logoUrl;
    private String status;
    private LocalDateTime createdAt;

    // Vai trò thành viên trong công ty (COMPANY_ADMIN hoặc RECRUITER)
    private String currentUserMemberRole;

    // Dữ liệu pháp lý nhạy cảm (chỉ hiển thị nếu currentUserMemberRole == COMPANY_ADMIN)
    private String taxCode;
    private String businessLicenseUrl;
    private LocalDateTime approvedAt;
    private String approvedByEmail;

    // Thống kê tổng hợp (tính bằng aggregate query có index, O(1), không N+1)
    private Long totalJobs;
    private Long activeJobs;
    private Long totalApplications;
    private Long totalMembers;
    private Long totalLocations;
    private Boolean hrmEnabled;
}
