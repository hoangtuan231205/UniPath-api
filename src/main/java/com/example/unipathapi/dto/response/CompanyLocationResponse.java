package com.example.unipathapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyLocationResponse {
    private Integer id;
    private Integer companyId;
    private String address;
    private double lat;
    private double lon;
}
