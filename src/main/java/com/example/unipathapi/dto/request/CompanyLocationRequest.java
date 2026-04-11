package com.example.unipathapi.dto.request;

import lombok.Data;

@Data
public class CompanyLocationRequest {
    private String address;
    private double lat;
    private double lon;
    private Integer companyId; // Bổ sung ID công ty để sau này dễ liên kết
}
