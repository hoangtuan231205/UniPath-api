package com.example.unipathapi.service;

import com.example.unipathapi.dto.response.CandidateSearchResponse;
import com.example.unipathapi.entity.CandidateProfile;
import com.example.unipathapi.repository.CandidateProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidateSearchService {

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    public List<CandidateSearchResponse> searchCandidates(String skill, String major, String university, String keyword) {
        List<CandidateProfile> profiles = candidateProfileRepository.findAll();

        return profiles.stream()
                .filter(p -> {
                    if (skill != null && !skill.trim().isEmpty()) {
                        if (p.getSkills() == null || !p.getSkills().toLowerCase().contains(skill.toLowerCase())) {
                            return false;
                        }
                    }
                    if (major != null && !major.trim().isEmpty()) {
                        if (p.getMajor() == null || !p.getMajor().toLowerCase().contains(major.toLowerCase())) {
                            return false;
                        }
                    }
                    if (university != null && !university.trim().isEmpty()) {
                        if (p.getUniversityName() == null || !p.getUniversityName().toLowerCase().contains(university.toLowerCase())) {
                            return false;
                        }
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.toLowerCase();
                        boolean matchName = p.getFullName() != null && p.getFullName().toLowerCase().contains(kw);
                        boolean matchMajor = p.getMajor() != null && p.getMajor().toLowerCase().contains(kw);
                        boolean matchUniv = p.getUniversityName() != null && p.getUniversityName().toLowerCase().contains(kw);
                        boolean matchSkills = p.getSkills() != null && p.getSkills().toLowerCase().contains(kw);
                        if (!matchName && !matchMajor && !matchUniv && !matchSkills) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(this::buildCandidateSearchResponse)
                .collect(Collectors.toList());
    }

    private CandidateSearchResponse buildCandidateSearchResponse(CandidateProfile p) {
        return CandidateSearchResponse.builder()
                .userId(p.getId())
                .fullName(p.getFullName())
                .universityName(p.getUniversityName())
                .major(p.getMajor())
                .experienceYears(p.getExperienceYears())
                .phoneNumber(p.getPhoneNumber())
                .skills(p.getSkills())
                .build();
    }
}
