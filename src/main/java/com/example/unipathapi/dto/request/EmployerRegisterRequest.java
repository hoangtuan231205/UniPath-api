package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployerRegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String phone;
    private String position;
    private String bio;

    // Company selection: exactly one of companyId or newCompany must be provided
    private Integer companyId;

    // In case of proposing a new company
    private String newCompanyName;
    private String newCompanyContactEmail;
    private String newCompanyTaxCode;
    private String newCompanyScale;
    private String newCompanyWebsite;
    private String newCompanyPhoneNumber;
    private String newCompanyDescription;

    private Boolean confirmed = false;
}
