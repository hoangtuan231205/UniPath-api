package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CvProfileRequest;
import com.example.unipathapi.service.CvProfileService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv-profiles")
@CrossOrigin(origins = "*")
public class CvProfileController {

    @Autowired
    private CvProfileService cvProfileService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<?> getMyCvProfiles(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(cvProfileService.getMyCvProfiles(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createCvProfile(@Valid @RequestBody CvProfileRequest cvRequest, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(cvProfileService.createCvProfile(userId, cvRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCvProfile(@PathVariable Integer id,
                                             @Valid @RequestBody CvProfileRequest cvRequest,
                                             HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(cvProfileService.updateCvProfile(id, userId, cvRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCvProfile(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            cvProfileService.deleteCvProfile(id, userId);
            return ResponseEntity.ok("Xoá CV thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/set-primary")
    public ResponseEntity<?> setPrimaryCvProfile(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(cvProfileService.setPrimaryCvProfile(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
