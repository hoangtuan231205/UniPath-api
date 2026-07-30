package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CvProfileRequest;
import com.example.unipathapi.dto.response.CvProfileResponse;
import com.example.unipathapi.entity.CvTemplate;
import com.example.unipathapi.entity.User;
import com.example.unipathapi.entity.UserCvProfile;
import com.example.unipathapi.repository.CvTemplateRepository;
import com.example.unipathapi.repository.UserCvProfileRepository;
import com.example.unipathapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CvProfileService {

    @Autowired
    private UserCvProfileRepository cvProfileRepository;

    @Autowired
    private CvTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    public List<CvProfileResponse> getMyCvProfiles(Integer userId) {
        List<UserCvProfile> profiles = cvProfileRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return profiles.stream().map(this::buildCvProfileResponse).collect(Collectors.toList());
    }

    public CvProfileResponse createCvProfile(Integer userId, CvProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        UserCvProfile cv = new UserCvProfile();
        cv.setUser(user);
        cv.setTitle(request.getTitle());
        cv.setContentJson(request.getContentJson());

        if (request.getTemplateId() != null) {
            CvTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElse(null);
            cv.setTemplate(template);
        }

        UserCvProfile saved = cvProfileRepository.save(cv);
        return buildCvProfileResponse(saved);
    }

    public CvProfileResponse updateCvProfile(Integer id, Integer userId, CvProfileRequest request) {
        UserCvProfile cv = cvProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa CV này");
        }

        cv.setTitle(request.getTitle());
        cv.setContentJson(request.getContentJson());

        if (request.getTemplateId() != null) {
            CvTemplate template = templateRepository.findById(request.getTemplateId()).orElse(null);
            cv.setTemplate(template);
        } else {
            cv.setTemplate(null);
        }

        UserCvProfile updated = cvProfileRepository.save(cv);
        return buildCvProfileResponse(updated);
    }

    public void deleteCvProfile(Integer id, Integer userId) {
        UserCvProfile cv = cvProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xoá CV này");
        }

        cvProfileRepository.delete(cv);
    }

    @Transactional
    public CvProfileResponse setPrimaryCvProfile(Integer id, Integer userId) {
        UserCvProfile cv = cvProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV"));

        if (!cv.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cài đặt cho CV này");
        }

        cvProfileRepository.resetPrimaryForUser(userId);
        cv.setIsPrimary(true);
        UserCvProfile saved = cvProfileRepository.save(cv);
        return buildCvProfileResponse(saved);
    }

    private CvProfileResponse buildCvProfileResponse(UserCvProfile cv) {
        return CvProfileResponse.builder()
                .id(cv.getId())
                .userId(cv.getUser().getId())
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
