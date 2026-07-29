package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CompanyJoinRequestResponse {
    private Integer id;
    private Integer userId;
    private String userName;
    private String userEmail;
    private Integer companyId;
    private String companyName;
    private String status;
    private String message;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private String reviewedByName;
}
