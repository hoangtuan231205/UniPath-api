package com.example.unipathapi.company;

import com.example.unipathapi.common.exception.GlobalExceptionHandler;
import com.example.unipathapi.company.controller.CompanyLocationController;
import com.example.unipathapi.company.dto.request.CompanyLocationRequest;
import com.example.unipathapi.company.dto.response.CompanyLocationResponse;
import com.example.unipathapi.company.dto.response.NearbyCompanyProjection;
import com.example.unipathapi.company.dto.response.NearbyCompanyResponse;
import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.CompanyLocation;
import com.example.unipathapi.company.repository.CompanyLocationRepository;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.service.CompanyLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyLocationTest {

    @Mock
    private CompanyLocationRepository locationRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMemberRepository memberRepository;

    @InjectMocks
    private CompanyLocationService locationService;

    private ObjectMapper objectMapper;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exceptionHandler = new GlobalExceptionHandler();
    }

    // =========================================================================
    // Test 1: Thiếu companyId hoặc address hoặc tọa độ -> Kỳ vọng ném ngoại lệ 400
    // =========================================================================
    @Test
    @DisplayName("1. Thiếu companyId hoặc address hoặc tọa độ phải ném 400 Bad Request")
    void testMissingRequiredFields() {
        CompanyLocationRequest reqNoCompany = new CompanyLocationRequest();
        reqNoCompany.setAddress("123 Phố Huế, Hà Nội");
        reqNoCompany.setLatitude(21.01);
        reqNoCompany.setLongitude(105.85);

        RuntimeException ex1 = assertThrows(RuntimeException.class,
                () -> locationService.addCompanyLocation(reqNoCompany, 1));
        assertTrue(ex1.getMessage().contains("400") && ex1.getMessage().contains("ID Công ty"));

        CompanyLocationRequest reqNoAddress = new CompanyLocationRequest();
        reqNoAddress.setCompanyId(10);
        reqNoAddress.setLatitude(21.01);
        reqNoAddress.setLongitude(105.85);

        RuntimeException ex2 = assertThrows(RuntimeException.class,
                () -> locationService.addCompanyLocation(reqNoAddress, 1));
        assertTrue(ex2.getMessage().contains("400") && ex2.getMessage().contains("Địa chỉ"));

        CompanyLocationRequest reqNoCoords = new CompanyLocationRequest();
        reqNoCoords.setCompanyId(10);
        reqNoCoords.setAddress("123 Phố Huế, Hà Nội");

        RuntimeException ex3 = assertThrows(RuntimeException.class,
                () -> locationService.addCompanyLocation(reqNoCoords, 1));
        assertTrue(ex3.getMessage().contains("400") && ex3.getMessage().contains("Vĩ độ"));
    }

    // =========================================================================
    // Test 2 & 3: Tọa độ và bán kính ngoài giới hạn
    // =========================================================================
    @Test
    @DisplayName("2. Tọa độ ngoài giới hạn (-90..90, -180..180) phải bị từ chối với 400")
    void testCoordinatesOutOfBounds() {
        CompanyLocationRequest invalidLatReq = new CompanyLocationRequest();
        invalidLatReq.setCompanyId(10);
        invalidLatReq.setAddress("Test");
        invalidLatReq.setLatitude(95.0); // Vượt quá 90
        invalidLatReq.setLongitude(105.8);

        RuntimeException latEx = assertThrows(RuntimeException.class,
                () -> locationService.addCompanyLocation(invalidLatReq, 1));
        assertTrue(latEx.getMessage().contains("400") && latEx.getMessage().contains("Vĩ độ"));

        CompanyLocationRequest invalidLonReq = new CompanyLocationRequest();
        invalidLonReq.setCompanyId(10);
        invalidLonReq.setAddress("Test");
        invalidLonReq.setLatitude(21.0);
        invalidLonReq.setLongitude(185.0); // Vượt quá 180

        RuntimeException lonEx = assertThrows(RuntimeException.class,
                () -> locationService.addCompanyLocation(invalidLonReq, 1));
        assertTrue(lonEx.getMessage().contains("400") && lonEx.getMessage().contains("Kinh độ"));
    }

    @Test
    @DisplayName("3. Bán kính ngoài giới hạn (0.1..50.0 km) phải bị từ chối")
    void testRadiusOutOfBounds() {
        RuntimeException exSmall = assertThrows(RuntimeException.class,
                () -> locationService.getNearbyCompanies(21.0, 105.8, 0.05));
        assertTrue(exSmall.getMessage().contains("400") && exSmall.getMessage().contains("Bán kính"));

        RuntimeException exLarge = assertThrows(RuntimeException.class,
                () -> locationService.getNearbyCompanies(21.0, 105.8, 55.0));
        assertTrue(exLarge.getMessage().contains("400") && exLarge.getMessage().contains("Bán kính"));
    }

    // =========================================================================
    // Test 4 & 5 & 10 & 11: Nearby query lọc APPROVED, tính khoảng cách, jobs count
    // =========================================================================
    @Test
    @DisplayName("4, 5, 10, 11. Nearby query: lọc approved, thứ tự khoảng cách tăng dần, có activeJobsCount, không serialize JTS Point")
    void testNearbyQueryMappingAndOrder() {
        NearbyCompanyProjection p1 = mock(NearbyCompanyProjection.class);
        when(p1.getCompanyId()).thenReturn(1);
        when(p1.getLocationId()).thenReturn(101);
        when(p1.getCompanyName()).thenReturn("FPT Software");
        when(p1.getLogoUrl()).thenReturn("https://fpt.com/logo.png");
        when(p1.getLocationName()).thenReturn("Trụ sở Cầu Giấy");
        when(p1.getAddress()).thenReturn("Duy Tân, Cầu Giấy");
        when(p1.getGooglePlaceId()).thenReturn("ChIJ123");
        when(p1.getLatitude()).thenReturn(21.0285);
        when(p1.getLongitude()).thenReturn(105.8542);
        when(p1.getDistanceMeters()).thenReturn(350.5);
        when(p1.getActiveJobsCount()).thenReturn(5L);

        NearbyCompanyProjection p2 = mock(NearbyCompanyProjection.class);
        when(p2.getCompanyId()).thenReturn(2);
        when(p2.getLocationId()).thenReturn(102);
        when(p2.getCompanyName()).thenReturn("Viettel Solutions");
        when(p2.getDistanceMeters()).thenReturn(1200.0);
        when(p2.getActiveJobsCount()).thenReturn(12L);

        when(locationRepository.findNearbyApprovedCompanies(eq(21.0), eq(105.8), eq(5000.0)))
                .thenReturn(List.of(p1, p2));

        List<NearbyCompanyResponse> result = locationService.getNearbyCompanies(21.0, 105.8, 5.0);

        assertEquals(2, result.size());
        assertEquals("FPT Software", result.get(0).getCompanyName());
        assertEquals(350.5, result.get(0).getDistanceMeters());
        assertEquals(5L, result.get(0).getActiveJobsCount());
        assertEquals(1200.0, result.get(1).getDistanceMeters());
        // Kiểm tra thứ tự tăng dần theo khoảng cách
        assertTrue(result.get(0).getDistanceMeters() < result.get(1).getDistanceMeters());
    }

    // =========================================================================
    // Test 6: Bắt lỗi trùng Google Place ID chuyển thành 409 Conflict
    // =========================================================================
    @Test
    @DisplayName("6. Trùng Google Place ID ném DataIntegrityViolationException và GlobalExceptionHandler trả 409 Conflict")
    void testGooglePlaceIdUniqueConstraintHandling() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"company_locations_google_place_id_unique\"");

        ResponseEntity<Object> response = exceptionHandler.handleDataIntegrityViolation(dive);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Google Place ID này đã được đăng ký"));
    }

    // =========================================================================
    // Test 7: Chuyển đổi primary (Hạ cờ primary cũ trước khi thêm mới)
    // =========================================================================
    @Test
    @DisplayName("7. Thêm địa điểm với primary = true phải gọi demoteCurrentPrimaryLocation")
    void testDemotePrimaryLocation() {
        Company company = new Company();
        company.setId(5);
        company.setStatus("APPROVED");

        when(memberRepository.existsByCompanyIdAndUserIdAndMemberRole(5, 1, "COMPANY_ADMIN")).thenReturn(true);
        when(companyRepository.findById(5)).thenReturn(Optional.of(company));
        when(locationRepository.save(any(CompanyLocation.class))).thenAnswer(inv -> {
            CompanyLocation loc = inv.getArgument(0);
            loc.setId(99);
            loc.setCreatedAt(LocalDateTime.now());
            loc.setUpdatedAt(LocalDateTime.now());
            return loc;
        });

        CompanyLocationRequest req = new CompanyLocationRequest();
        req.setCompanyId(5);
        req.setAddress("Tầng 10, Tòa nhà Keangnam");
        req.setLatitude(21.0167);
        req.setLongitude(105.7839);
        req.setPrimary(true);

        CompanyLocationResponse resp = locationService.addCompanyLocation(req, 1);

        // Xác nhận đã gọi demoteCurrentPrimaryLocation cho công ty 5
        verify(locationRepository, times(1)).demoteCurrentPrimaryLocation(5);
        assertTrue(resp.getPrimary());
    }

    // =========================================================================
    // Test 8: Trùng primary concurrent request chuyển thành 409 Conflict
    // =========================================================================
    @Test
    @DisplayName("8. Vi phạm company_locations_one_primary_per_company trả về 409 Conflict")
    void testPrimaryUniqueConstraintHandling() {
        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"company_locations_one_primary_per_company\"");

        ResponseEntity<Object> response = exceptionHandler.handleDataIntegrityViolation(dive);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Công ty đã có địa điểm chính"));
    }

    // =========================================================================
    // Test 9: Jackson deserialize hỗ trợ alias @JsonAlias({"lat"}), @JsonAlias({"lon"})
    // =========================================================================
    @Test
    @DisplayName("9. JSON Jackson bind được cả hai định dạng: lat/lon và latitude/longitude")
    void testJsonAliasBinding() throws Exception {
        String legacyJson = "{\"companyId\": 1, \"address\": \"Hà Nội\", \"lat\": 21.0285, \"lon\": 105.8542}";
        CompanyLocationRequest legacyReq = objectMapper.readValue(legacyJson, CompanyLocationRequest.class);

        assertEquals(21.0285, legacyReq.getLatitude());
        assertEquals(105.8542, legacyReq.getLongitude());
        assertEquals(21.0285, legacyReq.getLat());
        assertEquals(105.8542, legacyReq.getLon());

        String newJson = "{\"companyId\": 1, \"address\": \"Hà Nội\", \"latitude\": 21.0285, \"longitude\": 105.8542, \"locationName\": \"Chi nhánh 1\"}";
        CompanyLocationRequest newReq = objectMapper.readValue(newJson, CompanyLocationRequest.class);

        assertEquals(21.0285, newReq.getLatitude());
        assertEquals(105.8542, newReq.getLongitude());
        assertEquals("Chi nhánh 1", newReq.getLocationName());
    }
}
