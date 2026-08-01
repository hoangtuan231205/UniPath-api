package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CompanyLocationRequest;
import com.example.unipathapi.dto.response.CompanyLocationResponse;
import com.example.unipathapi.service.CompanyLocationService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "*")
public class CompanyLocationController {

    @Autowired
    private CompanyLocationService companyLocationService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/add")
    public ResponseEntity<?> addCompany(@RequestBody CompanyLocationRequest request, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            CompanyLocationResponse response = companyLocationService.addCompanyLocation(request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Lỗi hệ thống";
            if (msg.startsWith("403:")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg.substring(4).trim());
            }
            return ResponseEntity.badRequest().body(msg);
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<CompanyLocationResponse>> getNearbyCompanies(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5000") double radius
    ) {
        List<CompanyLocationResponse> responses = companyLocationService.getNearbyCompanies(lat, lon, radius);
        return ResponseEntity.ok(responses);
    }
}
