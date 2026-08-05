package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.JobRequest;
import com.example.unipathapi.service.JobService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.createJob(userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobDetail(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getOptionalCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.getJobDetail(id, currentUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Integer id,
                                       @Valid @RequestBody JobRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.updateJob(id, userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<?> closeJob(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.closeJob(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            jobService.deleteJob(id, userId);
            return ResponseEntity.ok("Xoá tin tuyển dụng thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<?> getFeedJobs(@RequestParam(required = false) Integer cursor,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer categoryId,
                                         @RequestParam(required = false) Integer locationId,
                                         @RequestParam(required = false) String jobType,
                                         HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getOptionalCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.getFeedJobs(cursor, keyword, categoryId, locationId, jobType, currentUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
