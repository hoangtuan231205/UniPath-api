package com.example.unipathapi.company.repository;

import com.example.unipathapi.company.dto.response.NearbyCompanyProjection;
import com.example.unipathapi.company.entity.CompanyLocation;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, Integer> {

    @Query(value = "SELECT " +
            "c.id AS \"companyId\", " +
            "cl.id AS \"locationId\", " +
            "c.company_name AS \"companyName\", " +
            "c.logo_url AS \"logoUrl\", " +
            "cl.location_name AS \"locationName\", " +
            "cl.address AS \"address\", " +
            "cl.google_place_id AS \"googlePlaceId\", " +
            "ST_Y(cl.geom) AS \"latitude\", " +
            "ST_X(cl.geom) AS \"longitude\", " +
            "ST_Distance(cl.geom::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS \"distanceMeters\", " +
            "COALESCE(job_counts.active_jobs_count, 0) AS \"activeJobsCount\" " +
            "FROM company_locations cl " +
            "JOIN companies c ON cl.company_id = c.id " +
            "LEFT JOIN ( " +
            "    SELECT j.company_id, COUNT(*) AS active_jobs_count " +
            "    FROM jobs j " +
            "    WHERE j.is_active = true " +
            "      AND (j.expired_at IS NULL OR j.expired_at > CURRENT_TIMESTAMP) " +
            "    GROUP BY j.company_id " +
            ") job_counts ON job_counts.company_id = c.id " +
            "WHERE c.status = 'APPROVED' " +
            "  AND cl.geom IS NOT NULL " +
            "  AND ST_DWithin( " +
            "      cl.geom::geography, " +
            "      ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, " +
            "      :radiusMeters " +
            "  ) " +
            "ORDER BY \"distanceMeters\" ASC",
            nativeQuery = true)
    List<NearbyCompanyProjection> findNearbyApprovedCompanies(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters
    );

    @Modifying
    @Query(value = "UPDATE company_locations SET is_primary = false, updated_at = CURRENT_TIMESTAMP " +
            "WHERE company_id = :companyId AND is_primary = true",
            nativeQuery = true)
    int demoteCurrentPrimaryLocation(@Param("companyId") Integer companyId);

    @Deprecated
    @Query(value = "SELECT * FROM company_locations " +
            "WHERE ST_DWithin(geom::geography, CAST(:userLocation AS geography), :radiusInMeters)",
            nativeQuery = true)
    List<CompanyLocation> findCompaniesWithinRadius(
            @Param("userLocation") Point userLocation,
            @Param("radiusInMeters") double radiusInMeters
    );
}
