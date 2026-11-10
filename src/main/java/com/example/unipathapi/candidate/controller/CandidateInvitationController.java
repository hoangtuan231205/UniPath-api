package com.example.unipathapi.candidate.controller;

import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.company.dto.request.DeclineInvitationRequest;
import com.example.unipathapi.company.service.EmploymentInvitationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidate/invitations")
@CrossOrigin(origins = "*")
public class CandidateInvitationController {

    @Autowired
    private EmploymentInvitationService invitationService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<?> getMyInvitations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest
    ) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.getCandidateInvitations(candidateId, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInvitationDetail(
            @PathVariable Integer id,
            HttpServletRequest httpRequest
    ) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.getCandidateInvitationDetail(candidateId, id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptInvitation(
            @PathVariable Integer id,
            HttpServletRequest httpRequest
    ) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.acceptInvitation(candidateId, id));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<?> declineInvitation(
            @PathVariable Integer id,
            @RequestBody(required = false) DeclineInvitationRequest request,
            HttpServletRequest httpRequest
    ) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        String reason = (request != null) ? request.getDeclineReason() : null;
        return ResponseEntity.ok(invitationService.declineInvitation(candidateId, id, reason));
    }

    @GetMapping("/employments")
    public ResponseEntity<?> getMyEmployments(HttpServletRequest httpRequest) {
        Integer candidateId = securityUtil.getCurrentUserId(httpRequest);
        return ResponseEntity.ok(invitationService.getCandidateEmployments(candidateId));
    }
}
