package com.example.unipathapi.controller;

import com.example.unipathapi.service.CompanyManagementService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private CompanyManagementService companyService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping("/me/companies")
    public ResponseEntity<?> getMyCompanies(HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(companyService.getUserCompanies(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
