package com.example.unipathapi.employer.dto.request;

import lombok.Data;

@Data

public class EmployerProfileRequest {
    private String fullName;
    private String phone;
    private String position;
    private String bio;
}
