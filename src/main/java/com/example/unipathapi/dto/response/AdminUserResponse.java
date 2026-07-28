package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AdminUserResponse {
    private Integer id;
    private String email;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String fullName;
}
