package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CandidateProfileRequest;
import com.example.unipathapi.dto.request.EmployerProfileRequest;
import com.example.unipathapi.service.ProfileService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> updateCandidateProfile(@PathVariable Integer userId,@Valid @RequestBody CandidateProfileRequest request) {
        try {
            return ResponseEntity.ok(profileService.updateCandidateProfile(userId, request));
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
    public ResponseEntity<?> updateEmployerProfile(@PathVariable Integer userId, @Valid @RequestBody EmployerProfileRequest request) {
        try {
            return ResponseEntity.ok(profileService.updateEmployerProfile(userId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}