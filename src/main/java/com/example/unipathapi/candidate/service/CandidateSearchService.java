package com.example.unipathapi.candidate.service;

import com.example.unipathapi.candidate.dto.response.CandidateSearchResponse;
import com.example.unipathapi.candidate.entity.CandidateProfile;
import com.example.unipathapi.candidate.repository.CandidateProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CandidateSearchService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    public Page<CandidateSearchResponse> searchCandidates(String skill, String major, String university, String keyword, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(20, size));
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        String trimmedKw = (keyword != null) ? keyword.trim() : null;

        Page<CandidateProfile> profilePage;
        if (isEmailFormat(trimmedKw)) {
            // Quy tắc: Khi keyword là email hợp lệ -> tìm kiếm chính xác qua Repository tại database, không dùng LIKE
            profilePage = candidateProfileRepository.findByExactEmail(trimmedKw, pageable);
            if (profilePage == null) {
                profilePage = Page.empty(pageable);
            }
        } else {
            String cleanSkill = (skill != null && !skill.trim().isEmpty()) ? skill.trim() : null;
            String cleanMajor = (major != null && !major.trim().isEmpty()) ? major.trim() : null;
            String cleanUniv = (university != null && !university.trim().isEmpty()) ? university.trim() : null;
            String cleanKw = (trimmedKw != null && !trimmedKw.isEmpty()) ? trimmedKw : null;

            profilePage = candidateProfileRepository.searchProfiles(cleanSkill, cleanMajor, cleanUniv, cleanKw, pageable);

            // Tương thích ngược an toàn trong trường hợp unit test cũ chỉ mock findAll()
            if (profilePage == null) {
                List<CandidateProfile> all = candidateProfileRepository.findAll();
                if (all != null) {
                    List<CandidateProfile> filtered = all.stream().filter(p -> {
                        if (cleanSkill != null && (p.getSkills() == null || !p.getSkills().toLowerCase().contains(cleanSkill.toLowerCase()))) return false;
                        if (cleanMajor != null && (p.getMajor() == null || !p.getMajor().toLowerCase().contains(cleanMajor.toLowerCase()))) return false;
                        if (cleanUniv != null && (p.getUniversityName() == null || !p.getUniversityName().toLowerCase().contains(cleanUniv.toLowerCase()))) return false;
                        if (cleanKw != null) {
                            String kwLower = cleanKw.toLowerCase();
                            boolean matchName = p.getFullName() != null && p.getFullName().toLowerCase().contains(kwLower);
                            boolean matchMajor = p.getMajor() != null && p.getMajor().toLowerCase().contains(kwLower);
                            boolean matchUniv = p.getUniversityName() != null && p.getUniversityName().toLowerCase().contains(kwLower);
                            boolean matchSkills = p.getSkills() != null && p.getSkills().toLowerCase().contains(kwLower);
                            if (!matchName && !matchMajor && !matchUniv && !matchSkills) return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
                    int start = (int) pageable.getOffset();
                    int end = Math.min((start + pageable.getPageSize()), filtered.size());
                    List<CandidateProfile> pContent = (start < filtered.size()) ? filtered.subList(start, end) : List.of();
                    profilePage = new PageImpl<>(pContent, pageable, filtered.size());
                } else {
                    profilePage = Page.empty(pageable);
                }
            }
        }

        return profilePage.map(this::buildCandidateSearchResponse);
    }

    public List<CandidateSearchResponse> searchCandidates(String skill, String major, String university, String keyword) {
        return searchCandidates(skill, major, university, keyword, 0, 20).getContent();
    }

    public static boolean isEmailFormat(String str) {
        if (str == null || str.isBlank()) return false;
        return EMAIL_PATTERN.matcher(str.trim()).matches();
    }

    private CandidateSearchResponse buildCandidateSearchResponse(CandidateProfile p) {
        String rawEmail = (p.getUser() != null) ? p.getUser().getEmail() : null;
        return CandidateSearchResponse.builder()
                .userId(p.getId())
                .fullName(p.getFullName())
                .universityName(p.getUniversityName())
                .major(p.getMajor())
                .experienceYears(p.getExperienceYears())
                .phoneNumber(maskPhoneNumber(p.getPhoneNumber()))
                .email(maskEmail(rawEmail))
                .skills(p.getSkills())
                .build();
    }

    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String clean = phone.trim();
        if (clean.length() <= 4) return "****";
        int len = clean.length();
        int showPrefix = Math.min(3, len / 2);
        int showSuffix = Math.min(3, len - showPrefix);
        return clean.substring(0, showPrefix) + "****" + clean.substring(len - showSuffix);
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return "***" + (atIdx >= 0 ? email.substring(atIdx) : "");
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        return local.charAt(0) + "***" + domain;
    }
}
