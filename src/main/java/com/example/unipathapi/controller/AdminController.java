package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CategoryRequest;
import com.example.unipathapi.dto.request.ReportRequest;
import com.example.unipathapi.dto.request.ReportResolveRequest;
import com.example.unipathapi.dto.request.ReviewJoinRequestDTO;
import com.example.unipathapi.dto.request.SkillRequest;
import com.example.unipathapi.service.AdminService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SecurityUtil securityUtil;

    // --- USERS MANAGEMENT ---
    @GetMapping("/api/admin/users")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String type,
                                      @RequestParam(required = false) Boolean status,
                                      @RequestParam(required = false) String search) {
        try {
            return ResponseEntity.ok(adminService.getUsers(type, status, search));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/api/admin/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(adminService.banUser(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/api/admin/users/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(adminService.unbanUser(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/api/admin/reports")
    public ResponseEntity<?> getReports(@RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(adminService.getReports(status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/api/admin/reports/{id}/resolve")
    public ResponseEntity<?> resolveReport(@PathVariable Integer id,
                                           @Valid @RequestBody ReportResolveRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(adminService.resolveReport(id, adminUserId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- CATEGORIES ---
    @GetMapping("/api/admin/categories")
    public ResponseEntity<?> getCategories() {
        try {
            return ResponseEntity.ok(adminService.getCategories());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/admin/categories")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryRequest request) {
        try {
            return ResponseEntity.ok(adminService.createCategory(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/api/admin/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Integer id, @Valid @RequestBody CategoryRequest request) {
        try {
            return ResponseEntity.ok(adminService.updateCategory(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Integer id) {
        try {
            adminService.deleteCategory(id);
            return ResponseEntity.ok("Xoá danh mục thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- SKILLS ---
    @GetMapping("/api/admin/skills")
    public ResponseEntity<?> getSkills() {
        try {
            return ResponseEntity.ok(adminService.getSkills());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/admin/skills")
    public ResponseEntity<?> createSkill(@Valid @RequestBody SkillRequest request) {
        try {
            return ResponseEntity.ok(adminService.createSkill(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/api/admin/skills/{id}")
    public ResponseEntity<?> updateSkill(@PathVariable Integer id, @Valid @RequestBody SkillRequest request) {
        try {
            return ResponseEntity.ok(adminService.updateSkill(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/admin/skills/{id}")
    public ResponseEntity<?> deleteSkill(@PathVariable Integer id) {
        try {
            adminService.deleteSkill(id);
            return ResponseEntity.ok("Xoá kỹ năng thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- STATS ---
    @GetMapping("/api/admin/stats")
    public ResponseEntity<?> getStats() {
        try {
            return ResponseEntity.ok(adminService.getStats());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- COMPANY PROPOSAL APPROVALS ---
    @GetMapping("/api/admin/companies/pending")
    public ResponseEntity<?> getPendingCompanies() {
        try {
            return ResponseEntity.ok(adminService.getPendingCompanies());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/api/admin/companies/{id}/review")
    public ResponseEntity<?> reviewCompanyProposal(@PathVariable Integer id,
                                                   @Valid @RequestBody ReviewJoinRequestDTO dto,
                                                   HttpServletRequest httpRequest) {
        try {
            Integer adminUserId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(adminService.reviewCompanyProposal(id, adminUserId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
