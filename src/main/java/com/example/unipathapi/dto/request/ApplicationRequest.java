package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationRequest {

    @NotNull(message = "Mã công việc không được để trống")
    private Integer jobId;

    private Integer cvProfileId;
    private String coverLetter;
}
