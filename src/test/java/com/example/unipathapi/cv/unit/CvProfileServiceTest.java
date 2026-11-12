package com.example.unipathapi.cv.unit;

import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.cv.dto.request.CvProfileRequest;
import com.example.unipathapi.cv.dto.response.CvProfileResponse;
import com.example.unipathapi.cv.entity.CvTemplate;
import com.example.unipathapi.cv.entity.UserCvProfile;
import com.example.unipathapi.cv.repository.CvTemplateRepository;
import com.example.unipathapi.cv.repository.UserCvProfileRepository;
import com.example.unipathapi.cv.service.CvProfileService;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CvProfileServiceTest {

    @Mock
    private UserCvProfileRepository cvProfileRepository;

    @Mock
    private CvTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private CvProfileService cvProfileService;

    private User candidateUser;
    private User anotherUser;
    private CvTemplate sampleTemplate;
    private UserCvProfile cv1;
    private UserCvProfile cv2;

    @BeforeEach
    void setUp() {
        candidateUser = new User();
        candidateUser.setId(10);
        candidateUser.setEmail("candidate@unipath.com");
        candidateUser.setRole("CANDIDATE");

        anotherUser = new User();
        anotherUser.setId(99);
        anotherUser.setEmail("other@unipath.com");
        anotherUser.setRole("CANDIDATE");

        sampleTemplate = new CvTemplate();
        sampleTemplate.setId(1);
        sampleTemplate.setName("Mẫu Tiêu chuẩn UniPath");

        cv1 = new UserCvProfile();
        cv1.setId(101);
        cv1.setUser(candidateUser);
        cv1.setTitle("CV Kỹ Sư Backend Java");
        cv1.setContentJson("{\"summary\":\"Kinh nghiệm 2 năm Java\"}");
        cv1.setIsPrimary(true);
        cv1.setTemplate(sampleTemplate);

        cv2 = new UserCvProfile();
        cv2.setId(102);
        cv2.setUser(candidateUser);
        cv2.setTitle("CV Fullstack Spring & React");
        cv2.setContentJson("{\"summary\":\"Fullstack developer\"}");
        cv2.setIsPrimary(false);
        cv2.setTemplate(sampleTemplate);
    }

    @Test
    @DisplayName("1. Tạo CV thành công - CV đầu tiên tự động đặt làm CV chính")
    void testCreateCv_FirstCv_SetsPrimary() {
        CvProfileRequest req = new CvProfileRequest();
        req.setTitle("CV Fresher Java");
        req.setContentJson("{\"skills\":[\"Java\"]}");
        req.setTemplateId(1);

        when(userRepository.findById(10)).thenReturn(Optional.of(candidateUser));
        when(templateRepository.findById(1)).thenReturn(Optional.of(sampleTemplate));
        when(cvProfileRepository.countByUserId(10)).thenReturn(0L);
        when(cvProfileRepository.save(any(UserCvProfile.class))).thenAnswer(inv -> {
            UserCvProfile p = inv.getArgument(0);
            p.setId(201);
            return p;
        });

        CvProfileResponse res = cvProfileService.createCvProfile(10, req);
        assertNotNull(res);
        assertEquals("CV Fresher Java", res.getTitle());
        assertTrue(res.getIsPrimary(), "CV đầu tiên của ứng viên phải tự động được đặt làm CV chính");
        verify(cvProfileRepository).resetPrimaryForUser(10);
    }

    @Test
    @DisplayName("2. Tạo CV với tiêu đề trống ném ResponseStatusException BAD_REQUEST")
    void testCreateCv_EmptyTitle_ThrowsBadRequest() {
        CvProfileRequest req = new CvProfileRequest();
        req.setTitle("   ");
        req.setContentJson("{}");

        when(userRepository.findById(10)).thenReturn(Optional.of(candidateUser));

        assertThrows(ResponseStatusException.class, () -> cvProfileService.createCvProfile(10, req));
    }

    @Test
    @DisplayName("3. Xem danh sách CV của ứng viên")
    void testGetMyCvProfiles() {
        when(cvProfileRepository.findByUserIdOrderByCreatedAtDesc(10)).thenReturn(List.of(cv1, cv2));

        List<CvProfileResponse> list = cvProfileService.getMyCvProfiles(10);
        assertEquals(2, list.size());
        assertEquals("CV Kỹ Sư Backend Java", list.get(0).getTitle());
    }

    @Test
    @DisplayName("4. Xem chi tiết CV của chính mình thành công")
    void testGetCvProfileById_Owner_Success() {
        when(cvProfileRepository.findById(101)).thenReturn(Optional.of(cv1));

        CvProfileResponse res = cvProfileService.getCvProfileById(101, 10);
        assertNotNull(res);
        assertEquals(101, res.getId());
    }

    @Test
    @DisplayName("5. Chỉnh sửa CV của mình thành công")
    void testUpdateCvProfile_Success() {
        CvProfileRequest req = new CvProfileRequest();
        req.setTitle("CV Senior Backend");
        req.setContentJson("{\"summary\":\"Updated\"}");

        when(cvProfileRepository.findById(101)).thenReturn(Optional.of(cv1));
        when(cvProfileRepository.save(any(UserCvProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CvProfileResponse res = cvProfileService.updateCvProfile(101, 10, req);
        assertEquals("CV Senior Backend", res.getTitle());
    }

    @Test
    @DisplayName("6 & 7. Đặt CV làm CV chính - Reset CV cũ và bật CV mới")
    void testSetPrimaryCvProfile_Success() {
        when(cvProfileRepository.findById(102)).thenReturn(Optional.of(cv2));
        when(cvProfileRepository.save(any(UserCvProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CvProfileResponse res = cvProfileService.setPrimaryCvProfile(102, 10);
        assertTrue(res.getIsPrimary());
        verify(cvProfileRepository).resetPrimaryForUser(10);
        verify(cvProfileRepository).save(cv2);
    }

    @Test
    @DisplayName("8. Xóa CV thường - Detach khỏi applications và xóa khỏi DB")
    void testDeleteCv_NonPrimary_Success() {
        when(cvProfileRepository.findById(102)).thenReturn(Optional.of(cv2));

        cvProfileService.deleteCvProfile(102, 10);

        verify(applicationRepository).detachCvProfileFromApplications(102);
        verify(cvProfileRepository).delete(cv2);
    }

    @Test
    @DisplayName("9. Xóa CV chính - Tự động kích hoạt CV còn lại làm CV chính")
    void testDeleteCv_Primary_ActivatesNextPrimary() {
        when(cvProfileRepository.findById(101)).thenReturn(Optional.of(cv1));
        when(cvProfileRepository.findByUserIdOrderByCreatedAtDesc(10)).thenReturn(List.of(cv2));

        cvProfileService.deleteCvProfile(101, 10);

        verify(applicationRepository).detachCvProfileFromApplications(101);
        verify(cvProfileRepository).delete(cv1);
        assertTrue(cv2.getIsPrimary(), "Khi CV chính bị xóa, CV còn lại phải trở thành CV chính mới");
        verify(cvProfileRepository).save(cv2);
    }

    @Test
    @DisplayName("10. Xem CV của user khác bị ném 403 FORBIDDEN")
    void testGetCvProfileById_OtherUser_ThrowsForbidden() {
        when(cvProfileRepository.findById(101)).thenReturn(Optional.of(cv1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> cvProfileService.getCvProfileById(101, 99));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("11. Thiết lập CV của user khác làm CV chính bị ném 403 FORBIDDEN")
    void testSetPrimaryCvProfile_OtherUser_ThrowsForbidden() {
        when(cvProfileRepository.findById(101)).thenReturn(Optional.of(cv1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> cvProfileService.setPrimaryCvProfile(101, 99));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("12. Xóa CV của user khác bị ném 403 FORBIDDEN")
    void testDeleteCv_OtherUser_ThrowsForbidden() {
        when(cvProfileRepository.findById(101)).thenReturn(Optional.of(cv1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> cvProfileService.deleteCvProfile(101, 99));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("13. Chỉnh sửa CV của user khác bị ném 403 FORBIDDEN")
    void testUpdateCv_OtherUser_ThrowsForbidden() {
        when(cvProfileRepository.findById(101)).thenReturn(Optional.of(cv1));

        CvProfileRequest req = new CvProfileRequest();
        req.setTitle("Hacked Title");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
            () -> cvProfileService.updateCvProfile(101, 99, req));
        assertEquals(403, ex.getStatusCode().value());
    }
}

