package com.example.unipathapi.company.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CompanyLocationRequest {

    @NotNull(message = "ID công ty không được để trống")
    private Integer companyId;

    @Size(max = 150, message = "Tên địa điểm không quá 150 ký tự")
    private String locationName;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @Size(max = 255, message = "Google Place ID không quá 255 ký tự")
    private String googlePlaceId;

    private Boolean primary = false;

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90.0 đến 90.0")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90.0 đến 90.0")
    @JsonAlias({"lat"})
    private Double latitude;

    @NotNull(message = "Kinh độ (longitude) không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180.0 đến 180.0")
    @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180.0 đến 180.0")
    @JsonAlias({"lon"})
    private Double longitude;

    // Helper getter để tương thích nếu code cũ còn gọi getLat/getLon
    public double getLat() {
        return latitude != null ? latitude : 0.0;
    }

    public double getLon() {
        return longitude != null ? longitude : 0.0;
    }

    // Explicit Getters and Setters
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
}
