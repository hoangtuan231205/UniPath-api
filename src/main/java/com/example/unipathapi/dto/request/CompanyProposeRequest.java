package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyProposeRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    private String companyName;

    private String contactEmail;
    private String taxCode;
    private String companyScale = "SME";
    private String phoneNumber;
    private String website;
    private String description;

    private Boolean confirmed = false;
}
