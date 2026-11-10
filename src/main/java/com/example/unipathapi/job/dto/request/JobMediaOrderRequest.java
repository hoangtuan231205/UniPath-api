package com.example.unipathapi.job.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobMediaOrderRequest {
    @NotEmpty(message = "Danh sách mediaIds không được để trống")
    private List<Integer> mediaIds;
}
