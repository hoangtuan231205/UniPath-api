package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.ShiftRequest;
import com.example.unipathapi.service.CompanyManagementService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employer")
@CrossOrigin(origins = "*")
public class EmploymentController {

    @Autowired
    private CompanyManagementService companyManagementService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping("/shifts/today")
    public ResponseEntity<?> getTodayShifts(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyManagementService.getTodayShifts(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/shifts")
    public ResponseEntity<?> createShift(@Valid @RequestBody ShiftRequest shiftRequest, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyManagementService.createShift(userId, shiftRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/shifts/{id}")
    public ResponseEntity<?> updateShift(@PathVariable Integer id,
                                         @Valid @RequestBody ShiftRequest shiftRequest,
                                         HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyManagementService.updateShift(id, userId, shiftRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/employees")
    public ResponseEntity<?> getEmployees(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyManagementService.getEmployees(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/payroll")
    public ResponseEntity<?> getPayroll(@RequestParam(required = false) Short month,
                                        @RequestParam(required = false) Short year,
                                        HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyManagementService.getPayroll(userId, month, year));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
