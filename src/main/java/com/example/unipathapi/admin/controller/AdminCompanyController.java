package com.example.unipathapi.admin.controller;
import com.example.unipathapi.admin.entity.*;
import com.example.unipathapi.admin.repository.*;
import com.example.unipathapi.admin.dto.request.*;
import com.example.unipathapi.admin.dto.response.*;
import com.example.unipathapi.admin.service.*;
import com.example.unipathapi.company.entity.*;
import com.example.unipathapi.company.repository.*;
import com.example.unipathapi.company.dto.request.*;
import com.example.unipathapi.company.dto.response.*;
import com.example.unipathapi.company.service.*;

import com.example.unipathapi.company.dto.request.CompanyRequest;
import jakarta.validation.Valid;
import com.example.unipathapi.admin.service.AdminService;
import com.example.unipathapi.common.security.SecurityUtil;
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

    @PostMapping
    public ResponseEntity<?> createCompanyByAdmin(@Valid @RequestBody CompanyRequest request, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateSuperAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.createCompanyByAdmin(request, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getCompaniesByStatus(@RequestParam(required = false, defaultValue = "PENDING") String status,
                                                  HttpServletRequest request) {
        try {
            validateAdminOrSuperAdminRole(request);
            return ResponseEntity.ok(adminService.getCompaniesByStatus(status));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PostMapping("/{id}/assign-admin")
    public ResponseEntity<?> assignCompanyAdmin(@PathVariable Integer id,
                                                @RequestParam Integer userId,
                                                @RequestParam(required = false, defaultValue = "COMPANY_ADMIN") String role,
                                                HttpServletRequest request) {
        try {
            Integer adminUserId = validateSuperAdminRole(request);
            return ResponseEntity.ok(adminService.addCompanyMemberByAdmin(id, userId, role, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveCompanyProposal(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer adminUserId = validateSuperAdminRole(request);
            return ResponseEntity.ok(adminService.approveCompanyProposal(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectCompanyProposal(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer adminUserId = validateSuperAdminRole(request);
            return ResponseEntity.ok(adminService.rejectCompanyProposal(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    private Integer validateSuperAdminRole(HttpServletRequest request) {
        String role = securityUtil.getCurrentUserRole(request);
        if (!"SUPERADMIN".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("403: Chỉ SUPERADMIN hoặc Quản trị viên cấp cao mới có quyền phê duyệt hoặc từ chối công ty");
        }
        return securityUtil.getCurrentUserId(request);
    }

    private Integer validateAdminOrSuperAdminRole(HttpServletRequest request) {
        String role = securityUtil.getCurrentUserRole(request);
        if (!"ADMIN".equalsIgnoreCase(role) && !"SUPERADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("403: Bạn không có quyền truy cập API quản trị này");
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
