package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportResolveRequest {

    @NotBlank(message = "Hành động xử lý không được để trống")
    private String action; // "REJECT", "DELETE_POST" (or DELETE_JOB), "BAN_ACCOUNT"
}
