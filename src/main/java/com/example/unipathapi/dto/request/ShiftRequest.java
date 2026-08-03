package com.example.unipathapi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ShiftRequest {

    @NotNull(message = "Mã nhân viên/hợp đồng không được để trống")
    private Integer employmentId;

    @NotNull(message = "Ngày làm việc không được để trống")
    private LocalDate shiftDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    private String status;
    private Integer locationId;
}
