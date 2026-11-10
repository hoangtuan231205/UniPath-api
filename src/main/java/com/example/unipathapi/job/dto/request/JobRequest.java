package com.example.unipathapi.job.dto.request;

import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JobRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotNull(message = "Loại hình công việc không được để trống")
    private JobType jobType;

    @NotNull(message = "Hình thức làm việc không được để trống")
    private WorkMode workMode;

    private String salaryRange;
    private String description;
    private String requirements; // String JSON format
    private Integer categoryId;
    private Integer locationId;
    private Integer companyId;
    private List<Integer> skillIds;

    @Future(message = "Hạn ứng tuyển không được ở quá khứ")
    private LocalDateTime expiredAt;
}

