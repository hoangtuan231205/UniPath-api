package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CompanyUpdateRequest;
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
}
