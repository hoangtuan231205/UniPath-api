package com.example.unipathapi.cv.service;

import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.cv.dto.request.CvProfileRequest;
import com.example.unipathapi.cv.dto.response.CvProfileResponse;
import com.example.unipathapi.cv.entity.CvTemplate;
import com.example.unipathapi.cv.entity.UserCvProfile;
import com.example.unipathapi.cv.repository.CvTemplateRepository;
import com.example.unipathapi.cv.repository.UserCvProfileRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CvProfileService {

    @Autowired
    private UserCvProfileRepository cvProfileRepository;

    @Autowired
    private CvTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    public List<CvProfileResponse> getMyCvProfiles(Integer userId) {
        List<UserCvProfile> profiles = cvProfileRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return profiles.stream().map(this::buildCvProfileResponse).collect(Collectors.toList());
    }

    public CvProfileResponse getCvProfileById(Integer id, Integer userId) {
        UserCvProfile cv = cvProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ CV"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập hồ sơ CV này");
        }

        return buildCvProfileResponse(cv);
    }

    public List<CvTemplate> getAvailableTemplates() {
        List<CvTemplate> templates = templateRepository.findAll();
        if (templates.isEmpty()) {
            CvTemplate t1 = new CvTemplate();
            t1.setName("Mẫu Tiêu chuẩn UniPath (Classic Standard)");
            t1.setLayoutJson("{\"theme\": \"classic\", \"font\": \"Inter\", \"color\": \"#4F46E5\"}");

            CvTemplate t2 = new CvTemplate();
            t2.setName("Mẫu Công nghệ & Lập trình (Modern Tech)");
            t2.setLayoutJson("{\"theme\": \"tech\", \"font\": \"Fira Code\", \"color\": \"#0284C7\"}");

            CvTemplate t3 = new CvTemplate();
            t3.setName("Mẫu Sáng tạo & Tối giản (Minimal Creative)");
            t3.setLayoutJson("{\"theme\": \"minimal\", \"font\": \"Plus Jakarta Sans\", \"color\": \"#7C3AED\"}");

            templateRepository.saveAll(List.of(t1, t2, t3));
            templates = templateRepository.findAll();
        }
        return templates;
    }

    public Map<String, Object> getPreFilledTemplateData(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        CandidateProfile profile = candidateProfileRepository.findById(userId).orElse(null);

        Map<String, Object> data = new LinkedHashMap<>();
        String fullName = profile != null && profile.getFullName() != null ? profile.getFullName() : "";
        String email = user.getEmail() != null ? user.getEmail() : "";
        String phone = profile != null && profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "";
        String university = profile != null && profile.getUniversityName() != null ? profile.getUniversityName() : "";
        String major = profile != null && profile.getMajor() != null ? profile.getMajor() : "";
        int expYears = profile != null && profile.getExperienceYears() != null ? profile.getExperienceYears() : 0;
        String avatarUrl = profile != null && profile.getAvatarUrl() != null ? profile.getAvatarUrl() : "";
        String skills = profile != null && profile.getSkills() != null ? profile.getSkills() : "";

        data.put("fullName", fullName);
        data.put("email", email);
        data.put("phone", phone);
        data.put("universityName", university);
        data.put("major", major);
        data.put("experienceYears", expYears);
        data.put("avatarUrl", avatarUrl);
        data.put("skills", skills);

        return data;
    }

    @Transactional
    public CvProfileResponse createCvProfile(Integer userId, CvProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiêu đề CV không được để trống");
        }

        UserCvProfile cv = new UserCvProfile();
        cv.setUser(user);
        cv.setTitle(request.getTitle().trim());
        cv.setContentJson(request.getContentJson() != null && !request.getContentJson().isBlank() ? request.getContentJson() : "{}");

        if (request.getTemplateId() != null) {
            CvTemplate template = templateRepository.findById(request.getTemplateId()).orElse(null);
            cv.setTemplate(template);
        }

        long existingCount = cvProfileRepository.countByUserId(userId);
        if (existingCount == 0 || Boolean.TRUE.equals(request.getIsPrimary())) {
            cvProfileRepository.resetPrimaryForUser(userId);
            cv.setIsPrimary(true);
        } else {
            cv.setIsPrimary(false);
        }

        UserCvProfile saved = cvProfileRepository.save(cv);
        return buildCvProfileResponse(saved);
    }

    @Transactional
    public CvProfileResponse updateCvProfile(Integer id, Integer userId, CvProfileRequest request) {
        UserCvProfile cv = cvProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ CV"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa hồ sơ CV này");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiêu đề CV không được để trống");
        }

        cv.setTitle(request.getTitle().trim());
        if (request.getContentJson() != null && !request.getContentJson().isBlank()) {
            cv.setContentJson(request.getContentJson());
        }

        if (request.getTemplateId() != null) {
            CvTemplate template = templateRepository.findById(request.getTemplateId()).orElse(null);
            cv.setTemplate(template);
        } else {
            cv.setTemplate(null);
        }

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            cvProfileRepository.resetPrimaryForUser(userId);
            cv.setIsPrimary(true);
        }

        UserCvProfile updated = cvProfileRepository.save(cv);
        return buildCvProfileResponse(updated);
    }

    @Transactional
    public void deleteCvProfile(Integer id, Integer userId) {
        UserCvProfile cv = cvProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ CV"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xoá hồ sơ CV này");
        }

        boolean wasPrimary = Boolean.TRUE.equals(cv.getIsPrimary());

        // Safely detach from applications, ensuring application history & snapshots are preserved
        applicationRepository.detachCvProfileFromApplications(id);

        cvProfileRepository.delete(cv);

        // If the deleted CV was primary, set the latest remaining CV as primary
        if (wasPrimary) {
            List<UserCvProfile> remaining = cvProfileRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                UserCvProfile nextPrimary = remaining.get(0);
                nextPrimary.setIsPrimary(true);
                cvProfileRepository.save(nextPrimary);
            }
        }
    }

    @Transactional
    public CvProfileResponse setPrimaryCvProfile(Integer id, Integer userId) {
        UserCvProfile cv = cvProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ CV"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cài đặt cho hồ sơ CV này");
        }

        cvProfileRepository.resetPrimaryForUser(userId);
        cv.setIsPrimary(true);
        UserCvProfile saved = cvProfileRepository.save(cv);
        return buildCvProfileResponse(saved);
    }

    private CvProfileResponse buildCvProfileResponse(UserCvProfile cv) {
        String fullName = null;
        try {
            CandidateProfile cp = candidateProfileRepository.findById(cv.getUser().getId()).orElse(null);
            if (cp != null && cp.getFullName() != null) {
                fullName = cp.getFullName();
            }
        } catch (Exception ignored) {}

        return CvProfileResponse.builder()
                .id(cv.getId())
                .userId(cv.getUser().getId())
                .fullName(fullName != null ? fullName : "Ứng viên")
                .templateId(cv.getTemplate() != null ? cv.getTemplate().getId() : null)
                .templateName(cv.getTemplate() != null ? cv.getTemplate().getName() : null)
                .title(cv.getTitle())
                .contentJson(cv.getContentJson())
                .isPrimary(cv.getIsPrimary())
                .createdAt(cv.getCreatedAt())
                .updatedAt(cv.getUpdatedAt())
                .build();
    }
}
