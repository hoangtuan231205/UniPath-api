package com.example.unipathapi.admin.controller;
import com.example.unipathapi.admin.entity.*;
import com.example.unipathapi.admin.repository.*;
import com.example.unipathapi.admin.dto.request.*;
import com.example.unipathapi.admin.dto.response.*;
import com.example.unipathapi.admin.service.*;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;
import com.example.unipathapi.company.entity.*;
import com.example.unipathapi.company.repository.*;
import com.example.unipathapi.company.dto.request.*;
import com.example.unipathapi.company.dto.response.*;
import com.example.unipathapi.company.service.*;

import com.example.unipathapi.admin.dto.request.AdminCreateUserRequest;
import com.example.unipathapi.job.dto.request.CategoryRequest;
import com.example.unipathapi.admin.dto.request.ReportRequest;
import com.example.unipathapi.admin.dto.request.ReportResolveRequest;
import com.example.unipathapi.job.dto.request.SkillRequest;
import com.example.unipathapi.admin.dto.request.UpdateMemberRoleRequest;
import com.example.unipathapi.admin.service.AdminAuditLogService;
import com.example.unipathapi.admin.service.AdminService;
import com.example.unipathapi.common.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminAuditLogService auditLogService;

    @Autowired
    private SecurityUtil securityUtil;

    // --- USERS ---
        @PostMapping("/api/admin/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody AdminCreateUserRequest request, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.createUser(request, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @GetMapping("/api/admin/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String type,
                                      @RequestParam(required = false) Boolean status,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size,
                                      HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.getUsers(type, status, search, page, size));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/admin/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.banUser(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/admin/users/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.unbanUser(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    // --- REPORTING ---
    @PostMapping("/api/jobs/{id}/report")
    public ResponseEntity<?> reportJob(@PathVariable Integer id,
                                       @Valid @RequestBody ReportRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(adminService.reportJob(id, userId, request));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PostMapping("/api/posts/{id}/report")
    public ResponseEntity<?> reportPost(@PathVariable Integer id,
                                        @Valid @RequestBody ReportRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(adminService.reportPost(id, userId, request));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @GetMapping("/api/admin/reports")
    public ResponseEntity<?> getReports(@RequestParam(required = false) String targetType,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size,
                                        HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.getReports(targetType, status, page, size));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/admin/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Integer id,
                                           @Valid @RequestBody ReportResolveRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.resolveReport(id, adminUserId, request));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    // --- CATEGORIES ---
    @GetMapping("/api/admin/categories")
    public ResponseEntity<?> getCategories() {
        try {
            return ResponseEntity.ok(adminService.getCategories());
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PostMapping("/api/admin/categories")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryRequest request, HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.createCategory(request));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Integer id,
                                            @Valid @RequestBody CategoryRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.updateCategory(id, request));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            adminService.deleteCategory(id, adminUserId);
            return ResponseEntity.ok("Xoá danh mục thành công");
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    // --- SKILLS ---
    @GetMapping("/api/admin/skills")
    public ResponseEntity<?> getSkills() {
        try {
            return ResponseEntity.ok(adminService.getSkills());
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PostMapping("/api/admin/skills")
    public ResponseEntity<?> createSkill(@Valid @RequestBody SkillRequest request, HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.createSkill(request));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PutMapping("/api/admin/skills/{id}")
    public ResponseEntity<?> updateSkill(@PathVariable Integer id,
                                         @Valid @RequestBody SkillRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.updateSkill(id, request));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/api/admin/skills/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            adminService.deleteSkill(id, adminUserId);
            return ResponseEntity.ok("Xoá kỹ năng thành công");
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    // --- STATS ---
    @GetMapping("/api/admin/stats")
    public ResponseEntity<?> getStats(HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.getStats());
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    // --- AUDIT LOGS ---
    @GetMapping("/api/admin/audit-logs")
    public ResponseEntity<?> getAuditLogs(@RequestParam(required = false) Integer adminId,
                                          @RequestParam(required = false) String targetType,
                                          @RequestParam(required = false, defaultValue = "0") Integer page,
                                          @RequestParam(required = false, defaultValue = "20") Integer size,
                                          HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(auditLogService.getLogs(adminId, targetType, page, size));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    // --- ORPHAN COMPANY JOIN REQUESTS ---
    @GetMapping("/api/admin/join-requests")
    public ResponseEntity<?> getOrphanJoinRequests(@RequestParam(required = false, defaultValue = "PENDING") String status,
                                                   HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.getPendingJoinRequestsForOrphanCompanies());
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/admin/join-requests/{id}/approve")
    public ResponseEntity<?> approveOrphanJoinRequest(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.approveOrphanCompanyJoinRequest(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/admin/join-requests/{id}/reject")
    public ResponseEntity<?> rejectOrphanJoinRequest(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.rejectOrphanCompanyJoinRequest(id, adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    // --- COMPANY MEMBERS INTERVENTION ---
    @GetMapping("/api/admin/companies/{id}/members")
    public ResponseEntity<?> getCompanyMembers(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.getCompanyMembers(id));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @PatchMapping("/api/admin/companies/{id}/members/{userId}/role")
    public ResponseEntity<?> updateCompanyMemberRole(@PathVariable Integer id,
                                                     @PathVariable Integer userId,
                                                     @Valid @RequestBody UpdateMemberRoleRequest request,
                                                     HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            return ResponseEntity.ok(adminService.updateCompanyMemberRole(id, userId, request.getMemberRole(), adminUserId));
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    @DeleteMapping("/api/admin/companies/{id}/members/{userId}")
    public ResponseEntity<?> removeCompanyMember(@PathVariable Integer id,
                                                 @PathVariable Integer userId,
                                                 HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = validateAdminRole(httpRequest);
            adminService.removeCompanyMember(id, userId, adminUserId);
            return ResponseEntity.ok("Gỡ bỏ thành viên khỏi công ty thành công");
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
