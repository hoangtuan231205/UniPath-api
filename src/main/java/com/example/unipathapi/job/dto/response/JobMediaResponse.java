package com.example.unipathapi.job.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMediaResponse {
    private Integer id;
    private Integer jobId;
    private String fileUrl;
    private String fileType; // "IMAGE" hoặc "VIDEO"
    private String mimeType;
    private Integer fileSizeBytes;
    private Integer displayOrder;
    private LocalDateTime uploadedAt;
}
