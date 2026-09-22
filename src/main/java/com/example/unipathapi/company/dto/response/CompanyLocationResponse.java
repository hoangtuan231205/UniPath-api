package com.example.unipathapi.company.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyLocationResponse {
    private Integer id;
    private Integer companyId;
    private String locationName;
    private String address;
    private String googlePlaceId;
    private Boolean primary;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor tương thích ngược
    public CompanyLocationResponse(Integer id, Integer companyId, String address, double lat, double lon) {
        this.id = id;
        this.companyId = companyId;
        this.address = address;
        this.latitude = lat;
        this.longitude = lon;
    }

    // Getter alias cho Frontend cũ
    public double getLat() {
        return latitude != null ? latitude : 0.0;
    }

    public double getLon() {
        return longitude != null ? longitude : 0.0;
    }

    // Explicit Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getGooglePlaceId() { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }

    public Boolean getPrimary() { return primary; }
    public void setPrimary(Boolean primary) { this.primary = primary; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
