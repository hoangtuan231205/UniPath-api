package com.example.unipathapi.user.service;

import com.example.unipathapi.candidate.dto.request.CandidateProfileRequest;
import com.example.unipathapi.employer.dto.request.EmployerProfileRequest;
import com.example.unipathapi.candidate.dto.response.CandidateProfileResponse;
import com.example.unipathapi.employer.dto.response.EmployerProfileResponse;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.employer.entity.EmployerProfile;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import com.example.unipathapi.employer.repository.EmployerProfileRepository;
import com.example.unipathapi.user.repository.UserRepository;
import com.example.unipathapi.common.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CandidateProfileRepository candidateRepo;
    @Autowired
    private EmployerProfileRepository employerRepo;
    @Autowired
    private FileStorageService fileStorageService;

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

    @Transactional
    public CandidateProfileResponse uploadCandidatePhoto(Integer userId, Integer currentUserId, String currentUserRole, MultipartFile file) {
        if (!userId.equals(currentUserId) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new RuntimeException("403: Bạn không có quyền cập nhật ảnh đại diện của người dùng khác");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        if (!"CANDIDATE".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Tài khoản này không phải là Ứng viên.");
        }

        CandidateProfile profile = candidateRepo.findByUserId(userId).orElse(new CandidateProfile());
        profile.setUser(user);

        String oldAvatar = profile.getAvatarUrl();
        String newAvatarUrl = fileStorageService.storeAvatar(file, "avatars/candidate");
        profile.setAvatarUrl(newAvatarUrl);
        candidateRepo.save(profile);

        // Xóa ảnh cũ an toàn nếu là ảnh do hệ thống lưu trữ
        if (oldAvatar != null && oldAvatar.startsWith("/uploads/avatars/")) {
            fileStorageService.deleteFile(oldAvatar);
        }

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
        res.setAvatarUrl(profile.getAvatarUrl());
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

        employerRepo.save(profile);
        return getEmployerProfile(userId);
    }

    @Transactional
    public EmployerProfileResponse uploadEmployerPhoto(Integer userId, Integer currentUserId, String currentUserRole, MultipartFile file) {
        if (!userId.equals(currentUserId) && !"ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new RuntimeException("403: Bạn không có quyền cập nhật ảnh đại diện của người dùng khác");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        if (!"EMPLOYER".equalsIgnoreCase(user.getRole())) {
            throw new RuntimeException("Tài khoản này không phải là Nhà tuyển dụng.");
        }

        EmployerProfile profile = employerRepo.findByUserId(userId).orElse(new EmployerProfile());
        profile.setUser(user);

        String oldAvatar = profile.getAvatarUrl();
        String newAvatarUrl = fileStorageService.storeAvatar(file, "avatars/employer");
        profile.setAvatarUrl(newAvatarUrl);
        employerRepo.save(profile);

        if (oldAvatar != null && oldAvatar.startsWith("/uploads/avatars/")) {
            fileStorageService.deleteFile(oldAvatar);
        }

        return getEmployerProfile(userId);
    }
}
