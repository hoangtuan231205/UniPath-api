package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CompanyMemberResponse {
    private Integer id;
    private Integer companyId;
    private String companyName;
    private Integer userId;
    private String fullName;
    private String email;
    private String memberRole; // 'COMPANY_ADMIN', 'RECRUITER'
    private LocalDateTime joinedAt;
}
