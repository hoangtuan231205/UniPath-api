package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    private String companyName;

    private String companyScale; // 'ENTERPRISE' hoặc 'SME'
    private String description;
    private String website;
    private String taxCode;
    private String phoneNumber;
    private String businessLicenseUrl;
}
