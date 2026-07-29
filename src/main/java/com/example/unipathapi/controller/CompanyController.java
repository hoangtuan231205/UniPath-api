package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CompanyCreateRequest;
import com.example.unipathapi.dto.request.CompanyJoinRequestDTO;
import com.example.unipathapi.dto.request.CompanyUpdateRequest;
import com.example.unipathapi.dto.request.ReviewJoinRequestDTO;
import com.example.unipathapi.service.CompanyManagementService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "*")
public class CompanyController {

    @Autowired
    private CompanyManagementService companyService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/create")
    public ResponseEntity<?> createCompanyProposal(@Valid @RequestBody CompanyCreateRequest createRequest, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.createCompanyProposal(userId, createRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyCompany(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.getMyCompany(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyCompany(@Valid @RequestBody CompanyUpdateRequest updateRequest, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.updateMyCompany(userId, updateRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/join-requests")
    public ResponseEntity<?> requestJoinCompany(@Valid @RequestBody CompanyJoinRequestDTO joinRequestDTO, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.requestJoinCompany(userId, joinRequestDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/join-requests")
    public ResponseEntity<?> getPendingJoinRequests(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.getPendingJoinRequests(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/join-requests/{id}/review")
    public ResponseEntity<?> reviewJoinRequest(@PathVariable Integer id,
                                               @Valid @RequestBody ReviewJoinRequestDTO reviewDTO,
                                               HttpServletRequest request) {
        try {
            Integer reviewerUserId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.reviewJoinRequest(id, reviewerUserId, reviewDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<?> getCompanyMembers(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(companyService.getCompanyMembers(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
