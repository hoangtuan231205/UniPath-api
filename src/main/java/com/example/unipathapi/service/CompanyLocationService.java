package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CompanyLocationRequest;
import com.example.unipathapi.dto.response.CompanyLocationResponse;
import com.example.unipathapi.entity.Company;
import com.example.unipathapi.entity.CompanyLocation;
import com.example.unipathapi.repository.CompanyLocationRepository;
import com.example.unipathapi.repository.CompanyMemberRepository;
import com.example.unipathapi.repository.CompanyRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyLocationService {

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository memberRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public CompanyLocationResponse addCompanyLocation(CompanyLocationRequest request, Integer userId) {
        if (request.getCompanyId() == null) {
            throw new RuntimeException("Phải cung cấp ID Công ty!");
        }

        boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(request.getCompanyId(), userId, "COMPANY_ADMIN");
        if (!isCompanyAdmin) {
            throw new RuntimeException("403: Bạn không có quyền quản lý công ty này");
        }

        Coordinate coordinate = new Coordinate(request.getLon(), request.getLat());
        Point point = geometryFactory.createPoint(coordinate);

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Công ty với ID: " + request.getCompanyId()));

        CompanyLocation location = new CompanyLocation();
        location.setAddress(request.getAddress());
        location.setGeom(point);
        location.setCompany(company);

        CompanyLocation savedLocation = locationRepository.save(location);

        return new CompanyLocationResponse(
                savedLocation.getId(),
                savedLocation.getCompany().getId(),
                savedLocation.getAddress(),
                savedLocation.getGeom().getY(),
                savedLocation.getGeom().getX()
        );
    }

    public List<CompanyLocationResponse> getNearbyCompanies(double lat, double lon, double radius) {
        Coordinate coordinate = new Coordinate(lon, lat);
        Point userLocation = geometryFactory.createPoint(coordinate);

        List<CompanyLocation> companies = locationRepository.findCompaniesWithinRadius(userLocation, radius);

        return companies.stream()
                .map(comp -> new CompanyLocationResponse(
                        comp.getId(),
                        comp.getCompany().getId(),
                        comp.getAddress(),
                        comp.getGeom().getY(),
                        comp.getGeom().getX()
                ))
                .collect(Collectors.toList());
    }
}
