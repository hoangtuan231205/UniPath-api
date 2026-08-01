package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JobRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String jobType;
    private String salaryRange;
    private String description;
    private String requirements; // String JSON format
    private Integer categoryId;
    private Integer locationId;
    private List<Integer> skillIds;
}
