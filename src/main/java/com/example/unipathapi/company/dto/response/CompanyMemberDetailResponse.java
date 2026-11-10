package com.example.unipathapi.company.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyMemberDetailResponse {
    private Integer id;
    private Integer userId;
    private String fullName;
    private String email;
    private String memberRole;
    private LocalDateTime joinedAt;
}
