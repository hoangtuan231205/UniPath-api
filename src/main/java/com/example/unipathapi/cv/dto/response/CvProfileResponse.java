package com.example.unipathapi.cv.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CvProfileResponse {
    private Integer id;
    private Integer userId;
    private String fullName;
    private Integer templateId;
    private String templateName;
    private String title;
    private String summary;
    private String contentJson;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
