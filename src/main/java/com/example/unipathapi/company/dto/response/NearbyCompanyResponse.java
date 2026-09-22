package com.example.unipathapi.company.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyCompanyResponse {
    private Integer companyId;
    private Integer locationId;
    private String companyName;
    private String logoUrl;
    private String locationName;
    private String address;
    private String googlePlaceId;
    private Double latitude;
    private Double longitude;
    private Double distanceMeters;

    /**
     * Tổng số lượng tin tuyển dụng đang hoạt động của toàn bộ công ty.
     * (Lưu ý: Đây là số việc làm của toàn công ty, không phải chỉ riêng chi nhánh này).
     */
    private Long activeJobsCount;

    // Explicit Getters and Setters
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public Integer getLocationId() { return locationId; }
    public void setLocationId(Integer locationId) { this.locationId = locationId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getGooglePlaceId() { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public Long getActiveJobsCount() { return activeJobsCount; }
    public void setActiveJobsCount(Long activeJobsCount) { this.activeJobsCount = activeJobsCount; }
}
