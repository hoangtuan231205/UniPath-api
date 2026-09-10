package com.example.unipathapi.employer.dto.response;

import lombok.Data;

@Data
public class EmployerProfileResponse {
    private Integer id;
    private String email;
    private String fullName;
    private String phone;
    private String position;
    private String bio;
}
