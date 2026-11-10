package com.example.unipathapi.common.storage;

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

    public static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    public static final long MAX_AVATAR_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    public static final long MAX_VIDEO_SIZE_BYTES = 50 * 1024 * 1024; // 50MB

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

    public String storeSingleJobMedia(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Tệp tin không được để trống");
        }

        // Chống path traversal trên subFolder
        if (subFolder.contains("..") || subFolder.contains("\\") || subFolder.startsWith("/")) {
            throw new RuntimeException("Đường dẫn lưu trữ không hợp lệ");
        }

        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        boolean isImageExt = Arrays.asList("jpg", "jpeg", "png", "webp").contains(extension);
        boolean isVideoExt = Arrays.asList("mp4", "webm").contains(extension);

        if (!isImageExt && !isVideoExt) {
            throw new RuntimeException("Chỉ chấp nhận tệp ảnh (JPG, PNG, WEBP) hoặc video (MP4, WEBM)");
        }

        if (isImageExt && file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new RuntimeException("Dung lượng ảnh vượt quá giới hạn 10MB (" 
                    + String.format("%.2f", file.getSize() / (1024.0 * 1024.0)) + " MB)");
        }

        if (isVideoExt && file.getSize() > MAX_VIDEO_SIZE_BYTES) {
            throw new RuntimeException("Dung lượng video vượt quá giới hạn 50MB (" 
                    + String.format("%.2f", file.getSize() / (1024.0 * 1024.0)) + " MB)");
        }

        // Kiểm tra Magic Numbers thực tế
        validateMagicBytes(file, extension);

        Path uploadPath = Paths.get(UPLOAD_BASE_DIR, subFolder).normalize();
        if (!uploadPath.startsWith(Paths.get(UPLOAD_BASE_DIR))) {
            throw new RuntimeException("Phát hiện nguy cơ Path Traversal");
        }

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file: " + e.getMessage());
        }

        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        Path filePath = uploadPath.resolve(storedFilename).normalize();

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String relativeUrl = "/" + UPLOAD_BASE_DIR + "/" + subFolder + "/" + storedFilename;
            return relativeUrl.replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi ghi tệp tin: " + e.getMessage());
        }
    }

    public String storeAvatar(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn một file hình ảnh hợp lệ.");
        }

        // Chống path traversal trên subFolder
        if (subFolder.contains("..") || subFolder.contains("\\") || subFolder.startsWith("/")) {
            throw new RuntimeException("Đường dẫn lưu trữ không hợp lệ");
        }

        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        boolean isImageExt = Arrays.asList("jpg", "jpeg", "png", "webp").contains(extension);

        if (!isImageExt) {
            throw new RuntimeException("Ảnh không đúng định dạng. Chỉ chấp nhận các định dạng JPG, JPEG, PNG, WEBP.");
        }

        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new RuntimeException("Kích thước ảnh không được vượt quá 5MB (" 
                    + String.format("%.2f", file.getSize() / (1024.0 * 1024.0)) + " MB)");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.toLowerCase().startsWith("image/")) {
            throw new RuntimeException("Vui lòng chọn một file hình ảnh hợp lệ.");
        }

        // Kiểm tra Magic Numbers thực tế
        validateMagicBytes(file, extension);

        Path uploadPath = Paths.get(UPLOAD_BASE_DIR, subFolder).normalize();
        if (!uploadPath.startsWith(Paths.get(UPLOAD_BASE_DIR))) {
            throw new RuntimeException("Phát hiện nguy cơ Path Traversal");
        }

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ file: " + e.getMessage());
        }

        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        Path filePath = uploadPath.resolve(storedFilename).normalize();

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String relativeUrl = "/" + UPLOAD_BASE_DIR + "/" + subFolder + "/" + storedFilename;
            return relativeUrl.replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi ghi tệp tin: " + e.getMessage());
        }
    }

    private void validateMagicBytes(MultipartFile file, String ext) {
        byte[] header = new byte[12];
        try (var is = file.getInputStream()) {
            int bytesRead = is.read(header);
            if (bytesRead < 4) {
                throw new RuntimeException("Tệp tin bị hỏng hoặc không có dữ liệu hợp lệ");
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc tệp tin: " + e.getMessage());
        }

        // JPEG: FF D8 FF
        if (ext.equals("jpg") || ext.equals("jpeg")) {
            if ((header[0] & 0xFF) != 0xFF || (header[1] & 0xFF) != 0xD8 || (header[2] & 0xFF) != 0xFF) {
                throw new RuntimeException("Nội dung tệp không phải định dạng JPEG hợp lệ");
            }
        }
        // PNG: 89 50 4E 47
        else if (ext.equals("png")) {
            if ((header[0] & 0xFF) != 0x89 || (header[1] & 0xFF) != 0x50 || (header[2] & 0xFF) != 0x4E || (header[3] & 0xFF) != 0x47) {
                throw new RuntimeException("Nội dung tệp không phải định dạng PNG hợp lệ");
            }
        }
        // WEBP: RIFF (bytes 0..3) and WEBP (bytes 8..11)
        else if (ext.equals("webp")) {
            boolean isRiff = (header[0] & 0xFF) == 0x52 && (header[1] & 0xFF) == 0x49 && (header[2] & 0xFF) == 0x46 && (header[3] & 0xFF) == 0x46;
            boolean isWebp = (header[8] & 0xFF) == 0x57 && (header[9] & 0xFF) == 0x45 && (header[10] & 0xFF) == 0x42 && (header[11] & 0xFF) == 0x50;
            if (!isRiff || !isWebp) {
                throw new RuntimeException("Nội dung tệp không phải định dạng WEBP hợp lệ");
            }
        }
        // MP4: bytes 4..7: 66 74 79 70 (ftyp)
        else if (ext.equals("mp4")) {
            if ((header[4] & 0xFF) != 0x66 || (header[5] & 0xFF) != 0x74 || (header[6] & 0xFF) != 0x79 || (header[7] & 0xFF) != 0x70) {
                throw new RuntimeException("Nội dung tệp không phải định dạng MP4 hợp lệ");
            }
        }
        // WEBM: 1A 45 DF A3
        else if (ext.equals("webm")) {
            if ((header[0] & 0xFF) != 0x1A || (header[1] & 0xFF) != 0x45 || (header[2] & 0xFF) != 0xDF || (header[3] & 0xFF) != 0xA3) {
                throw new RuntimeException("Nội dung tệp không phải định dạng WEBM hợp lệ");
            }
        }
    }

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
