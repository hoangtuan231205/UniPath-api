package com.example.unipathapi.company.dto.response;

public interface NearbyCompanyProjection {
    Integer getCompanyId();
    Integer getLocationId();
    String getCompanyName();
    String getLogoUrl();
    String getLocationName();
    String getAddress();
    String getGooglePlaceId();
    Double getLatitude();
    Double getLongitude();
    Double getDistanceMeters();
    Long getActiveJobsCount();
}
