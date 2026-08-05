package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.CandidateProfileRequest;
import com.example.unipathapi.dto.request.EmployerProfileRequest;
import com.example.unipathapi.dto.response.CandidateProfileResponse;
import com.example.unipathapi.dto.response.EmployerProfileResponse;
import com.example.unipathapi.entity.CandidateProfile;
import com.example.unipathapi.entity.EmployerProfile;
import com.example.unipathapi.entity.User;
import com.example.unipathapi.repository.CandidateProfileRepository;
import com.example.unipathapi.repository.EmployerProfileRepository;
import com.example.unipathapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CandidateProfileRepository candidateRepo;
    @Autowired
    private EmployerProfileRepository employerRepo;

    // ----- LOGIC CHO CANDIDATE -----
    public CandidateProfileResponse getCandidateProfile(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        if (!"CANDIDATE".equals(user.getRole())) {
            throw new RuntimeException("Truy cập bị từ chối! Tài khoản này không phải là Ứng viên.");
        }
        CandidateProfile profile = candidateRepo.findByUserId(userId).orElse(new CandidateProfile());

        CandidateProfileResponse res = new CandidateProfileResponse();
        res.setId(userId);
        res.setEmail(user.getEmail());
        res.setFullName(profile.getFullName());
        res.setPhoneNumber(profile.getPhoneNumber());
        res.setExperienceYears(profile.getExperienceYears());
        res.setUniversityName(profile.getUniversityName());
        res.setMajor(profile.getMajor());
        res.setSkills(profile.getSkills());
        res.setAvatarUrl(profile.getAvatarUrl());
        res.setCompletionPercentage(calculateCandidateCompletionPercentage(profile));
        return res;
    }

    public Object getProfileOfCurrentUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));
        if ("CANDIDATE".equalsIgnoreCase(user.getRole())) {
            return getCandidateProfile(userId);
        } else if ("EMPLOYER".equalsIgnoreCase(user.getRole())) {
            return getEmployerProfile(userId);
        } else {
            throw new RuntimeException("Vai trò không hỗ trợ lấy thông tin hồ sơ cá nhân");
        }
    }

    private Integer calculateCandidateCompletionPercentage(CandidateProfile profile) {
        int percentage = 0;
        if (profile.getFullName() != null && !profile.getFullName().isBlank()) percentage += 20;
        if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isBlank()) percentage += 15;
        if (profile.getExperienceYears() != null) percentage += 15;
        if (profile.getUniversityName() != null && !profile.getUniversityName().isBlank()) percentage += 15;
        if (profile.getMajor() != null && !profile.getMajor().isBlank()) percentage += 15;
        if (profile.getSkills() != null && !profile.getSkills().isBlank()) percentage += 10;
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) percentage += 10;
        return percentage;
    }

    @Transactional
    public CandidateProfileResponse updateCandidateProfile(Integer userId, CandidateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        if (!"CANDIDATE".equals(user.getRole())) {
            throw new RuntimeException("Truy cập bị từ chối! Tài khoản này không phải là Ứng viên.");
        }
        CandidateProfile profile = candidateRepo.findByUserId(userId).orElse(new CandidateProfile());

        profile.setUser(user); // Map ID từ User
        profile.setFullName(request.getFullName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setSkills(request.getSkills());
        profile.setUniversityName(request.getUniversityName());
        profile.setMajor(request.getMajor());

        candidateRepo.save(profile);
        return getCandidateProfile(userId);
    }

    // ----- LOGIC CHO EMPLOYER -----
    public EmployerProfileResponse getEmployerProfile(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        if (!"EMPLOYER".equals(user.getRole())) {
            throw new RuntimeException("Truy cập bị từ chối! Tài khoản này không phải là Nhà tuyển dụng.");
        }
        EmployerProfile profile = employerRepo.findByUserId(userId).orElse(new EmployerProfile());

        EmployerProfileResponse res = new EmployerProfileResponse();
        res.setId(userId);
        res.setEmail(user.getEmail());
        res.setFullName(profile.getFullName());
        res.setPhone(profile.getPhone());
        res.setPosition(profile.getPosition());
        res.setBio(profile.getBio());
        return res;
    }

    @Transactional
    public EmployerProfileResponse updateEmployerProfile(Integer userId, EmployerProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        if (!"EMPLOYER".equals(user.getRole())) {
            throw new RuntimeException("Truy cập bị từ chối! Tài khoản này không phải là Nhà tuyển dụng.");
        }
        EmployerProfile profile = employerRepo.findByUserId(userId).orElse(new EmployerProfile());

        profile.setUser(user);
        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setPosition(request.getPosition());
        profile.setBio(request.getBio());
        // Không cho phép user tự update isVerified, cái này do Admin/Trigger lo

        employerRepo.save(profile);
        return getEmployerProfile(userId);
    }
}