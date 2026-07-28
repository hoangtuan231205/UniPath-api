package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CompanyResponse {
    private Integer id;
    private Integer userId;
    private String companyName;
    private String companyScale;
    private String description;
    private String website;
}
