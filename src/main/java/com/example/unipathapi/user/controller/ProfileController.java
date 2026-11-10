package com.example.unipathapi.user.controller;

import com.example.unipathapi.candidate.dto.request.CandidateProfileRequest;
import com.example.unipathapi.employer.dto.request.EmployerProfileRequest;
import com.example.unipathapi.user.service.ProfileService;
import com.example.unipathapi.common.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private SecurityUtil securityUtil;

    // --- API LẤY PROFILE USER HIỆN TẠI ---
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(profileService.getProfileOfCurrentUser(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- API CHO ỨNG VIÊN (CANDIDATE) ---

    @GetMapping("/candidate/{userId}")
    public ResponseEntity<?> getCandidateProfile(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(profileService.getCandidateProfile(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/candidate/{userId}")
    public ResponseEntity<?> updateCandidateProfile(@PathVariable Integer userId,
                                                   @Valid @RequestBody CandidateProfileRequest request,
                                                   HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getCurrentUserId(httpRequest);
            String currentUserRole = securityUtil.getCurrentUserRole(httpRequest);
            if (!userId.equals(currentUserId) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền chỉnh sửa hồ sơ của người dùng khác");
            }
            return ResponseEntity.ok(profileService.updateCandidateProfile(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/candidate/{userId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCandidatePhoto(
            @PathVariable Integer userId,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        try {
            MultipartFile uploadFile = photo != null ? photo : file;
            if (uploadFile == null || uploadFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn ảnh đại diện để tải lên");
            }
            Integer currentUserId = securityUtil.getCurrentUserId(request);
            String currentUserRole = securityUtil.getCurrentUserRole(request);
            return ResponseEntity.ok(profileService.uploadCandidatePhoto(userId, currentUserId, currentUserRole, uploadFile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- API CHO NHÀ TUYỂN DỤNG (EMPLOYER) ---

    @GetMapping("/employer/{userId}")
    public ResponseEntity<?> getEmployerProfile(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(profileService.getEmployerProfile(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/employer/{userId}")
    public ResponseEntity<?> updateEmployerProfile(@PathVariable Integer userId,
                                                  @Valid @RequestBody EmployerProfileRequest request,
                                                  HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getCurrentUserId(httpRequest);
            String currentUserRole = securityUtil.getCurrentUserRole(httpRequest);
            if (!userId.equals(currentUserId) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền chỉnh sửa hồ sơ của người dùng khác");
            }
            return ResponseEntity.ok(profileService.updateEmployerProfile(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/employer/{userId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadEmployerPhoto(
            @PathVariable Integer userId,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        try {
            MultipartFile uploadFile = photo != null ? photo : file;
            if (uploadFile == null || uploadFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn ảnh đại diện để tải lên");
            }
            Integer currentUserId = securityUtil.getCurrentUserId(request);
            String currentUserRole = securityUtil.getCurrentUserRole(request);
            return ResponseEntity.ok(profileService.uploadEmployerPhoto(userId, currentUserId, currentUserRole, uploadFile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
