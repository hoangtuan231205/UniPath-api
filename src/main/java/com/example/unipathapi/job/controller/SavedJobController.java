package com.example.unipathapi.job.controller;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;

import com.example.unipathapi.job.service.SavedJobService;
import com.example.unipathapi.common.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class SavedJobController {

    @Autowired
    private SavedJobService savedJobService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/api/jobs/{id}/save")
    public ResponseEntity<?> saveJob(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer studentId = securityUtil.getCurrentUserId(request);
            savedJobService.saveJob(id, studentId);
            return ResponseEntity.ok("Đã lưu tin tuyển dụng");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/jobs/{id}/save")
    public ResponseEntity<?> unsaveJob(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer studentId = securityUtil.getCurrentUserId(request);
            savedJobService.unsaveJob(id, studentId);
            return ResponseEntity.ok("Đã bỏ lưu tin tuyển dụng");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/saved-jobs")
    public ResponseEntity<?> getSavedJobs(HttpServletRequest request) {
        try {
            Integer studentId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(savedJobService.getSavedJobs(studentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
