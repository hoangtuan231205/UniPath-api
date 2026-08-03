package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewJoinRequestDTO {

    @NotBlank(message = "Hành động xử lý không được để trống (APPROVE / REJECT)")
    private String action;
}
