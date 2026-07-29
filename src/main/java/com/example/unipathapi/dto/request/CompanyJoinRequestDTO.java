package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyJoinRequestDTO {

    @NotNull(message = "Mã công ty không được để trống")
    private Integer companyId;

    private String message;
}
