package com.example.unipathapi.company.controller;

import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.company.dto.request.EmploymentInvitationCreateRequest;
import com.example.unipathapi.company.service.EmploymentInvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employer/companies/{companyId}/invitations")
@CrossOrigin(origins = "*")
public class EmploymentInvitationController {

    @Autowired
    private EmploymentInvitationService invitationService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<?> createDirectInvitation(
            @PathVariable Integer companyId,
            @Valid @RequestBody EmploymentInvitationCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Integer employerUserId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.createDirectInvitation(employerUserId, companyId, request));
    }

    @PostMapping(value = {"/applications/{applicationId}", "/from-application/{applicationId}"})
    public ResponseEntity<?> createRecruitmentInvitation(
            @PathVariable Integer companyId,
            @PathVariable Integer applicationId,
            @Valid @RequestBody com.example.unipathapi.company.dto.request.RecruitmentInvitationCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Integer employerUserId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.createRecruitmentInvitationFromApplication(employerUserId, companyId, applicationId, request));
    }

    @GetMapping
    public ResponseEntity<?> getCompanyInvitations(
            @PathVariable Integer companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        Integer employerUserId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.getCompanyInvitations(employerUserId, companyId, status, page, size));
    }

    @RequestMapping(value = "/{invitationId}/cancel", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<?> cancelInvitation(
            @PathVariable Integer companyId,
            @PathVariable Integer invitationId,
            HttpServletRequest httpRequest
    ) {
        Integer employerUserId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.cancelInvitation(employerUserId, companyId, invitationId));
    }

    @PostMapping("/{invitationId}/onboard")
    public ResponseEntity<?> onboardCandidate(
            @PathVariable Integer companyId,
            @PathVariable Integer invitationId,
            HttpServletRequest httpRequest
    ) {
        Integer employerUserId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.onboardCandidate(employerUserId, companyId, invitationId));
    }
}
