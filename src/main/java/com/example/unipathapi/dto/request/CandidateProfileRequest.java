package com.example.unipathapi.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CandidateProfileRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    private Integer experienceYears;

    @NotBlank(message = "Kỹ năng không được để trống")
    private String skills;
    private String universityName;
    private String major;
}
