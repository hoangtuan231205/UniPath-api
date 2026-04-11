package com.example.unipathapi.controller;
import com.example.unipathapi.dto.request.CompanyLocationRequest;
import com.example.unipathapi.dto.response.CompanyLocationResponse;
import com.example.unipathapi.entity.CompanyLocation;
import com.example.unipathapi.repository.CompanyLocationRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies")
public class CompanyLocationController {

    @Autowired
    private CompanyLocationRepository repository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * API 1: THÊM CÔNG TY (DÙNG DTO REQUEST)
     */
    @PostMapping("/add")
    public CompanyLocationResponse addCompany(@RequestBody CompanyLocationRequest request) {
        // Vẫn nguyên tắc X trước, Y sau
        Coordinate coordinate = new Coordinate(request.getLon(), request.getLat());
        Point point = geometryFactory.createPoint(coordinate);

        CompanyLocation location = new CompanyLocation();
        location.setAddress(request.getAddress());
        location.setGeom(point);
        // Nếu Frontend có gửi companyId thì dùng, không thì tạm để 1
        location.setCompanyId(request.getCompanyId() != null ? request.getCompanyId() : 1);

        CompanyLocation savedLocation = repository.save(location);

        // Chuyển Entity thành Response DTO để trả về
        return new CompanyLocationResponse(
                savedLocation.getId(),
                savedLocation.getCompanyId(),
                savedLocation.getAddress(),
                savedLocation.getGeom().getY(), // Y là Vĩ độ (Lat)
                savedLocation.getGeom().getX()  // X là Kinh độ (Lon)
        );
    }

    /**
     * API 2: TÌM KIẾM BÁN KÍNH (TRẢ VỀ LIST DTO)
     */
    @GetMapping("/nearby")
    public List<CompanyLocationResponse> getNearbyCompanies(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5000") double radius
    ) {
        Coordinate coordinate = new Coordinate(lon, lat);
        Point userLocation = geometryFactory.createPoint(coordinate);

        List<CompanyLocation> companies = repository.findCompaniesWithinRadius(userLocation, radius);

        // Map danh sách Entity sang danh sách Response DTO
        return companies.stream()
                .map(comp -> new CompanyLocationResponse(
                        comp.getId(),
                        comp.getCompanyId(),
                        comp.getAddress(),
                        comp.getGeom().getY(),
                        comp.getGeom().getX()
                ))
                .collect(Collectors.toList());
    }
}
