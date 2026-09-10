package com.example.unipathapi.employer.controller;
import com.example.unipathapi.company.entity.*;
import com.example.unipathapi.company.repository.*;
import com.example.unipathapi.company.dto.request.*;
import com.example.unipathapi.company.dto.response.*;
import com.example.unipathapi.company.service.*;

import com.example.unipathapi.company.dto.request.EmploymentRequest;
import com.example.unipathapi.company.dto.request.ShiftRequest;
import com.example.unipathapi.company.service.CompanyManagementService;
import com.example.unipathapi.common.security.SecurityUtil;
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

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployment(@Valid @RequestBody EmploymentRequest employmentRequest, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyManagementService.createEmployment(userId, employmentRequest));
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
