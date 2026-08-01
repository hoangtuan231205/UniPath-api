package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillRequest {

    @NotBlank(message = "Tên kỹ năng không được để trống")
    private String name;
}
