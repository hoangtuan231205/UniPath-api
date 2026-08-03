package com.example.unipathapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class ShiftResponse {
    private Integer id;
    private Integer employmentId;
    private Integer studentId;
    private String studentName;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Integer locationId;
    private String locationAddress;
}
