package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {
    @NotBlank(message = "Vai trò thành viên không được để trống")
    private String memberRole; // 'COMPANY_ADMIN', 'RECRUITER'
}
