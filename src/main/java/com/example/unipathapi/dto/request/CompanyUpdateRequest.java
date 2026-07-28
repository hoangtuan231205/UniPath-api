package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyUpdateRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    private String companyName;

    private String companyScale; // ENTERPRISE or SME
    private String description;
    private String website;
}
