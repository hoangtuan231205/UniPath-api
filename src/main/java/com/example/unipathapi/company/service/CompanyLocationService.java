package com.example.unipathapi.company.service;

import com.example.unipathapi.company.dto.request.CompanyLocationRequest;
import com.example.unipathapi.company.dto.response.CompanyLocationResponse;
import com.example.unipathapi.company.dto.response.NearbyCompanyProjection;
import com.example.unipathapi.company.dto.response.NearbyCompanyResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyLocation;
import com.example.unipathapi.company.repository.CompanyLocationRepository;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
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
            throw new RuntimeException("400: Phải cung cấp ID Công ty!");
        }
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new RuntimeException("400: Địa chỉ không được để trống!");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new RuntimeException("400: Vĩ độ và kinh độ không được để trống!");
        }
        if (request.getLatitude() < -90.0 || request.getLatitude() > 90.0) {
            throw new RuntimeException("400: Vĩ độ phải nằm trong khoảng từ -90.0 đến 90.0!");
        }
        if (request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
            throw new RuntimeException("400: Kinh độ phải nằm trong khoảng từ -180.0 đến 180.0!");
        }

        boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(request.getCompanyId(), userId, "COMPANY_ADMIN");
        if (!isCompanyAdmin) {
            throw new RuntimeException("403: Bạn không có quyền quản lý công ty này");
        }

        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("404: Không tìm thấy Công ty với ID: " + request.getCompanyId()));

        boolean isPrimary = Boolean.TRUE.equals(request.getPrimary());
        if (isPrimary) {
            locationRepository.demoteCurrentPrimaryLocation(company.getId());
        }

        Coordinate coordinate = new Coordinate(request.getLongitude(), request.getLatitude());
        Point point = geometryFactory.createPoint(coordinate);

        CompanyLocation location = new CompanyLocation();
        location.setCompany(company);
        location.setLocationName(request.getLocationName());
        location.setAddress(request.getAddress().trim());
        location.setGeom(point);
        location.setGooglePlaceId(request.getGooglePlaceId());
        location.setPrimary(isPrimary);

        CompanyLocation savedLocation = locationRepository.save(location);

        return CompanyLocationResponse.builder()
                .id(savedLocation.getId())
                .companyId(savedLocation.getCompany().getId())
                .locationName(savedLocation.getLocationName())
                .address(savedLocation.getAddress())
                .googlePlaceId(savedLocation.getGooglePlaceId())
                .primary(savedLocation.isPrimary())
                .latitude(savedLocation.getGeom().getY())
                .longitude(savedLocation.getGeom().getX())
                .createdAt(savedLocation.getCreatedAt())
                .updatedAt(savedLocation.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<NearbyCompanyResponse> getNearbyCompanies(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null) {
            throw new RuntimeException("400: Vĩ độ (latitude) không được để trống!");
        }
        if (longitude == null) {
            throw new RuntimeException("400: Kinh độ (longitude) không được để trống!");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new RuntimeException("400: Vĩ độ phải nằm trong khoảng từ -90.0 đến 90.0!");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new RuntimeException("400: Kinh độ phải nằm trong khoảng từ -180.0 đến 180.0!");
        }
        if (radiusKm == null) {
            radiusKm = 5.0;
        }
        if (radiusKm < 0.1 || radiusKm > 50.0) {
            throw new RuntimeException("400: Bán kính tìm kiếm phải từ 0.1 km đến 50.0 km!");
        }

        double radiusMeters = radiusKm * 1000.0;
        List<NearbyCompanyProjection> projections = locationRepository.findNearbyApprovedCompanies(latitude, longitude, radiusMeters);

        return projections.stream()
                .map(p -> NearbyCompanyResponse.builder()
                        .companyId(p.getCompanyId())
                        .locationId(p.getLocationId())
                        .companyName(p.getCompanyName())
                        .logoUrl(p.getLogoUrl())
                        .locationName(p.getLocationName())
                        .address(p.getAddress())
                        .googlePlaceId(p.getGooglePlaceId())
                        .latitude(p.getLatitude())
                        .longitude(p.getLongitude())
                        .distanceMeters(p.getDistanceMeters())
                        .activeJobsCount(p.getActiveJobsCount())
                        .build())
                .collect(Collectors.toList());
    }
}
