package com.example.unipathapi.company.controller;

import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.company.dto.request.CompanyLocationRequest;
import com.example.unipathapi.company.dto.response.CompanyLocationResponse;
import com.example.unipathapi.company.dto.response.NearbyCompanyResponse;
import com.example.unipathapi.company.service.CompanyLocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Endpoint chuẩn RESTful mới để thêm địa điểm công ty.
     * Chỉ COMPANY_ADMIN của công ty mới có quyền truy cập.
     */
    @PostMapping("/locations")
    public ResponseEntity<CompanyLocationResponse> createCompanyLocation(
            @Valid @RequestBody CompanyLocationRequest request,
            HttpServletRequest httpRequest
    ) {
        Integer userId = securityUtil.getCurrentUserId(httpRequest);
        CompanyLocationResponse response = companyLocationService.addCompanyLocation(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint cũ giữ lại để tương thích ngược 100% với Frontend/Postman hiện tại.
     * @deprecated Hãy chuyển sang sử dụng {@link #createCompanyLocation(CompanyLocationRequest, HttpServletRequest)}.
     */
    @Deprecated
    @PostMapping("/add")
    public ResponseEntity<CompanyLocationResponse> addCompany(
            @Valid @RequestBody CompanyLocationRequest request,
            HttpServletRequest httpRequest
    ) {
        Integer userId = securityUtil.getCurrentUserId(httpRequest);
        CompanyLocationResponse response = companyLocationService.addCompanyLocation(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Tìm kiếm danh sách công ty lân cận theo tọa độ và bán kính.
     * Hỗ trợ cả 2 bộ tham số:
     * - Bộ tham số chuẩn mới: latitude, longitude, radiusKm
     * - Bộ tham số cũ: lat, lon, radius (mét)
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyCompanyResponse>> getNearbyCompanies(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) Double radius
    ) {
        Double finalLat = latitude != null ? latitude : lat;
        Double finalLon = longitude != null ? longitude : lon;
        Double finalRadiusKm = radiusKm != null ? radiusKm : (radius != null ? radius / 1000.0 : 5.0);

        List<NearbyCompanyResponse> responses = companyLocationService.getNearbyCompanies(finalLat, finalLon, finalRadiusKm);
        return ResponseEntity.ok(responses);
    }
}
