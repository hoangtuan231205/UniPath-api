package com.example.unipathapi.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
    private static final int MAX_FILES_PER_BATCH = 10;
    private static final String UPLOAD_BASE_DIR = "uploads";

    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    ));

    public List<String> storeFiles(List<MultipartFile> files, String subFolder) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter out empty files
        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return Collections.emptyList();
        }

        if (validFiles.size() > MAX_FILES_PER_BATCH) {
            throw new RuntimeException("Tối đa chỉ được đính kèm " + MAX_FILES_PER_BATCH + " file mỗi lần upload");
        }

        // --- STEP 1: ATOMIC PRE-VALIDATION (Check ALL files before saving any) ---
        for (MultipartFile file : validFiles) {
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new RuntimeException("Kích thước file '" + file.getOriginalFilename() + "' vượt quá giới hạn 2MB (" 
                        + String.format("%.2f", file.getSize() / (1024.0 * 1024.0)) + " MB)");
            }

            String contentType = file.getContentType();
            String extension = getFileExtension(file.getOriginalFilename());
            
            boolean isAllowedMime = contentType != null && ALLOWED_MIME_TYPES.contains(contentType.toLowerCase());
            boolean isAllowedExt = Arrays.asList("jpg", "jpeg", "png", "webp", "gif", "pdf", "doc", "docx", "txt").contains(extension.toLowerCase());

            if (!isAllowedMime && !isAllowedExt) {
                throw new RuntimeException("Định dạng file '" + file.getOriginalFilename() + "' không được hỗ trợ (Chấp nhận: JPG, PNG, WEBP, GIF, PDF, DOC, DOCX)");
            }
        }

        // --- STEP 2: WRITE FILES TO DISK ---
        Path uploadPath = Paths.get(UPLOAD_BASE_DIR, subFolder);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file: " + e.getMessage());
        }

        List<String> fileUrls = new ArrayList<>();

        for (MultipartFile file : validFiles) {
            String extension = getFileExtension(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
            Path filePath = uploadPath.resolve(storedFilename);

            try {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                String relativeUrl = "/" + UPLOAD_BASE_DIR + "/" + subFolder + "/" + storedFilename;
                fileUrls.add(relativeUrl.replace("\\", "/"));
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi ghi file '" + file.getOriginalFilename() + "': " + e.getMessage());
            }
        }

        return fileUrls;
    }

    public void deleteFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) return;
        try {
            String cleanUrl = fileUrl.startsWith("/") ? fileUrl.substring(1) : fileUrl;
            Path filePath = Paths.get(cleanUrl);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception e) {
            System.err.println("Could not delete file " + fileUrl + ": " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) return "";
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
