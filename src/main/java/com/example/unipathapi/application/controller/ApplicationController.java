package com.example.unipathapi.application.controller;
import com.example.unipathapi.application.entity.*;
import com.example.unipathapi.application.repository.*;
import com.example.unipathapi.application.dto.request.*;
import com.example.unipathapi.application.dto.response.*;
import com.example.unipathapi.application.service.*;

import com.example.unipathapi.application.dto.request.UpdateApplicationStatusRequest;
import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.service.ApplicationService;
import com.example.unipathapi.common.security.SecurityUtil;
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
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(applicationService.checkApplied(id, candidateId));
    }

    @PostMapping(value = "/api/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> applyJob(
            @RequestParam("jobId") Integer jobId,
            @RequestParam(value = "coverLetter", required = false) String coverLetter,
            @RequestParam(value = "cvProfileId", required = false) Integer cvProfileId,
            @RequestPart(value = "cvFile", required = false) MultipartFile cvFile,
            HttpServletRequest httpRequest
    ) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(applicationService.applyJob(candidateId, jobId, coverLetter, cvProfileId, cvFile));
    }

    @PatchMapping("/api/applications/{id}/withdraw")
    public ResponseEntity<?> withdrawApplication(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(applicationService.withdrawApplication(id, candidateId));
    }

    @GetMapping("/api/applications/me")
    public ResponseEntity<?> getMyApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(applicationService.getMyApplications(candidateId, status, page, size));
    }

    @GetMapping("/api/applications")
    public ResponseEntity<?> getCompanyApplications(
            @RequestParam(required = false) Integer jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        Integer employerId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(applicationService.getCompanyApplications(employerId, jobId, status, page, size));
    }

    @PatchMapping("/api/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        Integer employerId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, employerId, request));
    }

    @GetMapping("/api/applications/{id}/cv/download")
    public ResponseEntity<?> downloadCv(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer currentUserId = securityUtil.getCurrentUserId(httpRequest);
        Resource resource = applicationService.getCvResource(id, currentUserId);
        Application app = applicationService.getApplicationById(id);

        String filename = app.getCvFilename() != null ? app.getCvFilename() : "CV_" + id + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
