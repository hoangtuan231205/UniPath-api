package com.example.unipathapi.company.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CompanyResponse {
    private Integer id;
    private String companyName;
    private String taxCode;
    private String phoneNumber;
    private String businessLicenseUrl;
    private String companyScale;
    private String description;
    private String website;
    private String status;
    private Integer createdById;
    private String createdByEmail;
    private Integer approvedById;
    private String approvedByEmail;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;

    // Company Admin & Staff info
    private Integer companyAdminUserId;
    private String companyAdminEmail;
    private String companyAdminFullName;
    private Integer totalMembers;
}
