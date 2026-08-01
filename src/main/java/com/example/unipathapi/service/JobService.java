package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.JobRequest;
import com.example.unipathapi.dto.response.JobResponse;
import com.example.unipathapi.entity.*;
import com.example.unipathapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMemberRepository memberRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private JobCategoryRepository categoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private JobLikeRepository jobLikeRepository;

    @Autowired
    private JobCommentRepository jobCommentRepository;

    @Autowired
    private JobShareRepository jobShareRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Transactional
    public JobResponse createJob(Integer userId, JobRequest request) {
        List<CompanyMember> members = memberRepository.findByUserId(userId);
        if (members.isEmpty()) {
            throw new RuntimeException("Bạn chưa thuộc công ty nào trên hệ thống. Không thể đăng tin tuyển dụng.");
        }

        Company company = members.get(0).getCompany();
        if (!"APPROVED".equalsIgnoreCase(company.getStatus())) {
            throw new RuntimeException("Công ty của bạn chưa được System Admin duyệt");
        }

        Job job = new Job();
        job.setCompany(company);
        job.setTitle(request.getTitle());
        job.setJobType(request.getJobType());
        job.setSalaryRange(request.getSalaryRange());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setIsActive(false); // Mặc định chờ duyệt/kích hoạt

        if (request.getCategoryId() != null) {
            JobCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
            job.setCategory(category);
        }

        if (request.getLocationId() != null) {
            CompanyLocation location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Vị trí địa lý không tồn tại"));
            job.setLocation(location);
        }

        Job savedJob = jobRepository.save(job);

        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            for (Integer skillId : request.getSkillIds()) {
                Skill skill = skillRepository.findById(skillId).orElse(null);
                if (skill != null) {
                    JobSkill jobSkill = new JobSkill(savedJob, skill);
                    jobSkillRepository.save(jobSkill);
                }
            }
        }

        return buildJobResponse(savedJob);
    }

    public JobResponse getJobDetail(Integer id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));
        return buildJobResponse(job);
    }

    @Transactional
    public JobResponse updateJob(Integer id, Integer userId, JobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        job.setTitle(request.getTitle());
        job.setJobType(request.getJobType());
        job.setSalaryRange(request.getSalaryRange());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());

        if (request.getCategoryId() != null) {
            JobCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
            job.setCategory(category);
        } else {
            job.setCategory(null);
        }

        if (request.getLocationId() != null) {
            CompanyLocation location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Vị trí không tồn tại"));
            job.setLocation(location);
        } else {
            job.setLocation(null);
        }

        jobSkillRepository.deleteByJobId(job.getId());
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            for (Integer skillId : request.getSkillIds()) {
                Skill skill = skillRepository.findById(skillId).orElse(null);
                if (skill != null) {
                    JobSkill jobSkill = new JobSkill(job, skill);
                    jobSkillRepository.save(jobSkill);
                }
            }
        }

        Job updatedJob = jobRepository.save(job);
        return buildJobResponse(updatedJob);
    }

    public JobResponse closeJob(Integer id, Integer userId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        job.setIsActive(false);
        Job savedJob = jobRepository.save(job);
        return buildJobResponse(savedJob);
    }

    @Transactional
    public void deleteJob(Integer id, Integer userId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        long applicationsCount = applicationRepository.countByJobId(id);
        if (applicationsCount > 0) {
            throw new RuntimeException("Không thể xoá tin đã có người ứng tuyển");
        }

        jobSkillRepository.deleteByJobId(id);
        jobRepository.delete(job);
    }

    public List<JobResponse> getFeedJobs(Integer cursor, String keyword, Integer categoryId, Integer locationId, String jobType) {
        Pageable pageable = PageRequest.of(0, 20);
        List<Job> jobs = jobRepository.findFeedJobs(cursor, keyword, categoryId, locationId, jobType, pageable);
        return jobs.stream().map(this::buildJobResponse).collect(Collectors.toList());
    }

    public JobResponse buildJobResponse(Job job) {
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        List<String> skillNames = jobSkills.stream()
                .map(js -> js.getSkill().getName())
                .collect(Collectors.toList());

        long likesCount = jobLikeRepository.countByJobId(job.getId());
        long commentsCount = jobCommentRepository.countByJobId(job.getId());
        long sharesCount = jobShareRepository.countByJobId(job.getId());
        long applicationsCount = applicationRepository.countByJobId(job.getId());

        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .jobType(job.getJobType())
                .salaryRange(job.getSalaryRange())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .categoryId(job.getCategory() != null ? job.getCategory().getId() : null)
                .categoryName(job.getCategory() != null ? job.getCategory().getName() : null)
                .locationId(job.getLocation() != null ? job.getLocation().getId() : null)
                .locationAddress(job.getLocation() != null ? job.getLocation().getAddress() : null)
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .companyName(job.getCompany() != null ? job.getCompany().getCompanyName() : null)
                .companyScale(job.getCompany() != null ? job.getCompany().getCompanyScale() : null)
                .skills(skillNames)
                .likesCount(likesCount)
                .commentsCount(commentsCount)
                .sharesCount(sharesCount)
                .applicationsCount(applicationsCount)
                .isActive(job.getIsActive())
                .postedAt(job.getPostedAt())
                .expiredAt(job.getExpiredAt())
                .type("JOB")
                .build();
    }

    private void validateEmployerCompanyAccess(Integer companyId, Integer userId) {
        Optional<CompanyMember> memberOpt = memberRepository.findByCompanyIdAndUserId(companyId, userId);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Bạn không có quyền quản lý tin tuyển dụng của công ty này");
        }
    }
}
