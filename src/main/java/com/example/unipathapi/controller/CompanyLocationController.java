package com.example.unipathapi.controller;
import com.example.unipathapi.dto.request.CompanyLocationRequest;
import com.example.unipathapi.dto.response.CompanyLocationResponse;
import com.example.unipathapi.service.CompanyLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyLocationController {

    @Autowired
    private CompanyLocationService companyLocationService;

    @PostMapping("/add")
    public ResponseEntity<?> addCompany(@RequestBody CompanyLocationRequest request) {
        try {
            CompanyLocationResponse response = companyLocationService.addCompanyLocation(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
