package com.example.unipathapi.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostMediaResponse {
    private Integer id;
    private Integer postId;
    private String fileUrl;
    private String fileType;
    private String mimeType;
    private Integer fileSizeBytes;
    private Integer displayOrder;
    private LocalDateTime uploadedAt;
}
