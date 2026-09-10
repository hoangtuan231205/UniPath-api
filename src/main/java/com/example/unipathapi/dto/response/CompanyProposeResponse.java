package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CompanyProposeResponse {
    private Boolean success;
    private Boolean duplicate;
    private Boolean warning;
    private Integer companyId;
    private String companyName;
    private String message;
    private List<SimilarCompanyDTO> similarCompanies;

    @Getter
    @Setter
    @Builder
    public static class SimilarCompanyDTO {
        private Integer id;
        private String companyName;
        private Double similarity;
    }
}
