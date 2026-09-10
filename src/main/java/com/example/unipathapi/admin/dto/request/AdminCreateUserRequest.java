package com.example.unipathapi.admin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    private String fullName;

    @NotBlank(message = "Vai trò không được để trống")
    private String role;

    private Integer companyId;

    private String companyRole;
}
