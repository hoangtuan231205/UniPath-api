package com.example.unipathapi.controller;

import com.example.unipathapi.service.AdminService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/companies")
@CrossOrigin(origins = "*")
public class AdminCompanyController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<?> getCompaniesByStatus(@RequestParam(required = false, defaultValue = "PENDING") String status,
                                                  HttpServletRequest request) {
        try {
            validateAdminRole(request);
            return ResponseEntity.ok(adminService.getCompaniesByStatus(status));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveCompanyProposal(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer adminUserId = validateAdminRole(request);
            return ResponseEntity.ok(adminService.approveCompanyProposal(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectCompanyProposal(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer adminUserId = validateAdminRole(request);
            return ResponseEntity.ok(adminService.rejectCompanyProposal(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    private Integer validateAdminRole(HttpServletRequest request) {
        String role = securityUtil.getCurrentUserRole(request);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("403: Chỉ System Admin mới có quyền truy cập API này");
        }
        return securityUtil.getCurrentUserId(request);
    }

    private ResponseEntity<?> handleException(RuntimeException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống";
        if (msg.startsWith("403:")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg.substring(4).trim());
        }
        return ResponseEntity.badRequest().body(msg);
    }
}
