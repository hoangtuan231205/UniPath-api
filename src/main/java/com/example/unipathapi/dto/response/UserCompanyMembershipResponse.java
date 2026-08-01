package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserCompanyMembershipResponse {
    private Integer companyId;
    private String companyName;
    private String taxCode;
    private String companyScale;
    private String description;
    private String website;
    private String status;
    private String memberRole; // 'COMPANY_ADMIN', 'RECRUITER'
    private LocalDateTime joinedAt;
}
