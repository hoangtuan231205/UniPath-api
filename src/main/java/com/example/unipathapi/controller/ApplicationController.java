package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.ApplicationRequest;
import com.example.unipathapi.dto.request.UpdateApplicationStatusRequest;
import com.example.unipathapi.entity.Application;
import com.example.unipathapi.service.ApplicationService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping("/api/jobs/{id}/check-applied")
    public ResponseEntity<?> checkApplied(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer studentId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(applicationService.checkApplied(id, studentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/api/applications", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> applyJob(@Valid @RequestPart("data") ApplicationRequest request,
                                      @RequestPart(value = "cvFile", required = false) MultipartFile cvFile,
                                      HttpServletRequest httpRequest) {
        try {
            Integer studentId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(applicationService.applyJob(studentId, request, cvFile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/applications/{id}")
    public ResponseEntity<?> withdrawApplication(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer studentId = securityUtil.getCurrentUserId(httpRequest);
            applicationService.withdrawApplication(id, studentId);
            return ResponseEntity.ok("Rút hồ sơ thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/applications/me")
    public ResponseEntity<?> getMyApplications(@RequestParam(required = false) String status, HttpServletRequest httpRequest) {
        try {
            Integer studentId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(applicationService.getMyApplications(studentId, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/jobs/{id}/applications")
    public ResponseEntity<?> getJobApplications(@PathVariable Integer id,
                                                @RequestParam(required = false) String status,
                                                HttpServletRequest httpRequest) {
        try {
            Integer employerId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(applicationService.getJobApplications(id, employerId, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/api/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(@PathVariable Integer id,
                                                     @Valid @RequestBody UpdateApplicationStatusRequest request,
                                                     HttpServletRequest httpRequest) {
        try {
            Integer employerId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(applicationService.updateApplicationStatus(id, employerId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/applications/{id}/cv/download")
    public ResponseEntity<?> downloadCv(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getCurrentUserId(httpRequest);
            Resource resource = applicationService.getCvResource(id, currentUserId);
            Application app = applicationService.getApplicationById(id);

            String filename = app.getCvFilename() != null ? app.getCvFilename() : "CV_" + id + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
