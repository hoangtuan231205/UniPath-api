package com.example.unipathapi.job.controller;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;

import com.example.unipathapi.application.service.ApplicationService;
import com.example.unipathapi.job.dto.request.JobRequest;
import com.example.unipathapi.job.service.JobService;
import com.example.unipathapi.common.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(jobService.createJob(userId, request));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/{id}/media", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadJobMedia(
            @PathVariable Integer id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest
    ) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(jobService.uploadJobMedia(id, userId, file));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<?> deleteJobMedia(@PathVariable Integer mediaId, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            jobService.deleteJobMedia(mediaId, userId);
            return ResponseEntity.ok("Đã xóa tệp đính kèm thành công");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/media/order")
    public ResponseEntity<?> updateMediaOrder(
            @PathVariable Integer id,
            @Valid @RequestBody com.example.unipathapi.job.dto.request.JobMediaOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.updateMediaOrder(id, userId, request.getMediaIds()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/employer")
    public ResponseEntity<?> getEmployerJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.getEmployerJobs(userId, keyword, status, sort, page, size));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/employer/stats")
    public ResponseEntity<?> getEmployerJobStats(HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.getEmployerJobStats(userId));
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

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<?> reopenJob(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.reopenJob(id, userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{jobId}/applications")
    public ResponseEntity<?> getJobApplications(
            @PathVariable Integer jobId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(applicationService.getApplicationsByJobId(jobId, userId, status, page, size));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
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
                                         @RequestParam(required = false) String workMode,
                                         HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getOptionalCurrentUserId(httpRequest);
            return ResponseEntity.ok(jobService.getFeedJobs(cursor, keyword, categoryId, locationId, jobType, workMode, currentUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
