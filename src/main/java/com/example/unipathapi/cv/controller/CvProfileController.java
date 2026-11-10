package com.example.unipathapi.cv.controller;

import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.cv.dto.request.CvProfileRequest;
import com.example.unipathapi.cv.service.CvProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/cv-profiles")
@CrossOrigin(origins = "*")
public class CvProfileController {

    @Autowired
    private CvProfileService cvProfileService;

    @Autowired
    private SecurityUtil securityUtil;

    private void enforceCandidateRole(HttpServletRequest request) {
        String role = securityUtil.getCurrentUserRole(request);
        if (role != null && !"CANDIDATE".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ ứng viên mới có quyền sử dụng chức năng quản lý CV");
        }
    }

    @GetMapping({"", "/me"})
    public ResponseEntity<?> getMyCvProfiles(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            enforceCandidateRole(request);
            return ResponseEntity.ok(cvProfileService.getMyCvProfiles(userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCvProfileById(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            enforceCandidateRole(request);
            return ResponseEntity.ok(cvProfileService.getCvProfileById(id, userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<?> getAvailableTemplates() {
        return ResponseEntity.ok(cvProfileService.getAvailableTemplates());
    }

    @GetMapping("/prefill")
    public ResponseEntity<?> getPreFilledTemplateData(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            enforceCandidateRole(request);
            return ResponseEntity.ok(cvProfileService.getPreFilledTemplateData(userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCvProfile(@Valid @RequestBody CvProfileRequest cvRequest, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            enforceCandidateRole(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(cvProfileService.createCvProfile(userId, cvRequest));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCvProfile(@PathVariable Integer id,
                                             @Valid @RequestBody CvProfileRequest cvRequest,
                                             HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            enforceCandidateRole(request);
            return ResponseEntity.ok(cvProfileService.updateCvProfile(id, userId, cvRequest));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCvProfile(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            enforceCandidateRole(request);
            cvProfileService.deleteCvProfile(id, userId);
            return ResponseEntity.ok(Map.of("message", "Xoá CV thành công"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/set-primary")
    public ResponseEntity<?> setPrimaryCvProfile(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            enforceCandidateRole(request);
            return ResponseEntity.ok(cvProfileService.setPrimaryCvProfile(id, userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}
