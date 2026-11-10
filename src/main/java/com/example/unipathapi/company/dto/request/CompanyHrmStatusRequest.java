package com.example.unipathapi.company.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyHrmStatusRequest {
    @NotNull(message = "Trạng thái hrmEnabled không được để trống")
    private Boolean hrmEnabled;
}
