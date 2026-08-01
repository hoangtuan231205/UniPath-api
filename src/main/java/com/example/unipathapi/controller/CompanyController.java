package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CompanyJoinRequestDTO;
import com.example.unipathapi.dto.request.CompanyRequest;
import com.example.unipathapi.service.CompanyManagementService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class CompanyController {

    @Autowired
    private CompanyManagementService companyService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/api/companies")
    public ResponseEntity<?> createCompany(@Valid @RequestBody CompanyRequest companyRequest, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.createCompany(userId, companyRequest));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @GetMapping("/api/companies/search")
    public ResponseEntity<?> searchCompanies(@RequestParam(required = false, defaultValue = "") String keyword) {
        try {
            return ResponseEntity.ok(companyService.searchCompanies(keyword));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @GetMapping("/api/companies/{id}")
    public ResponseEntity<?> getCompanyDetail(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer currentUserId = null;
            String currentUserRole = null;
            try {
                currentUserId = securityUtil.getCurrentUserId(request);
                currentUserRole = securityUtil.getCurrentUserRole(request);
            } catch (Exception ignored) {}

            return ResponseEntity.ok(companyService.getCompanyDetail(id, currentUserId, currentUserRole));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PutMapping("/api/companies/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Integer id,
                                           @Valid @RequestBody CompanyRequest companyRequest,
                                           HttpServletRequest request) {
        try {
            Integer currentUserId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.updateCompany(id, currentUserId, companyRequest));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PostMapping("/api/companies/{id}/join-requests")
    public ResponseEntity<?> requestJoinCompany(@PathVariable Integer id,
                                               @RequestBody(required = false) CompanyJoinRequestDTO joinRequestDTO,
                                               HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            CompanyJoinRequestDTO dto = joinRequestDTO != null ? joinRequestDTO : new CompanyJoinRequestDTO();
            return ResponseEntity.ok(companyService.requestJoinCompany(id, userId, dto));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @GetMapping("/api/companies/{id}/join-requests")
    public ResponseEntity<?> getPendingJoinRequestsOfCompany(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer currentUserId = securityUtil.getCurrentUserId(request);
            String currentUserRole = securityUtil.getCurrentUserRole(request);
            return ResponseEntity.ok(companyService.getPendingJoinRequestsOfCompany(id, currentUserId, currentUserRole));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/join-requests/{id}/approve")
    public ResponseEntity<?> approveJoinRequest(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer reviewerUserId = securityUtil.getCurrentUserId(request);
            String reviewerRole = securityUtil.getCurrentUserRole(request);
            return ResponseEntity.ok(companyService.approveJoinRequest(id, reviewerUserId, reviewerRole));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/join-requests/{id}/reject")
    public ResponseEntity<?> rejectJoinRequest(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer reviewerUserId = securityUtil.getCurrentUserId(request);
            String reviewerRole = securityUtil.getCurrentUserRole(request);
            return ResponseEntity.ok(companyService.rejectJoinRequest(id, reviewerUserId, reviewerRole));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @GetMapping("/api/companies/{id}/members")
    public ResponseEntity<?> getCompanyMembers(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(companyService.getCompanyMembers(id));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    private ResponseEntity<?> handleException(RuntimeException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống";
        if (msg.startsWith("403:")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg.substring(4).trim());
        }
        return ResponseEntity.badRequest().body(msg);
    }
}
