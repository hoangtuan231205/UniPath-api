package com.example.unipathapi.repository;
import com.example.unipathapi.entity.CompanyLocation;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, Integer> {

    // Sử dụng Native Query với hàm cast chuẩn của PostGIS
    @Query(value = "SELECT * FROM company_locations " +
            "WHERE ST_DWithin(geom::geography, CAST(:userLocation AS geography), :radiusInMeters)",
            nativeQuery = true)
    List<CompanyLocation> findCompaniesWithinRadius(
            @Param("userLocation") Point userLocation, // Hibernate Spatial sẽ tự chuyển Point thành dữ liệu DB
            @Param("radiusInMeters") double radiusInMeters
    );
}
