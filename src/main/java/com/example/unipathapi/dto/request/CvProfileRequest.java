package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CvProfileRequest {

    private Integer templateId;

    @NotBlank(message = "Tiêu đề CV không được để trống")
    private String title;

    @NotBlank(message = "Nội dung JSON của CV không được để trống")
    private String contentJson;
}
