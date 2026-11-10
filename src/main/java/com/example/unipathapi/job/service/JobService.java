package com.example.unipathapi.job.service;
import com.example.unipathapi.application.entity.Application;
import com.example.unipathapi.application.repository.ApplicationRepository;
import com.example.unipathapi.job.entity.*;
import com.example.unipathapi.job.repository.*;
import com.example.unipathapi.job.dto.request.*;
import com.example.unipathapi.job.dto.response.*;
import com.example.unipathapi.job.service.*;
import com.example.unipathapi.company.entity.*;
import com.example.unipathapi.company.repository.*;
import com.example.unipathapi.company.dto.request.*;
import com.example.unipathapi.company.dto.response.*;
import com.example.unipathapi.company.service.*;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;

import com.example.unipathapi.job.dto.request.JobRequest;
import com.example.unipathapi.job.dto.response.EmployerJobManagementResponse;
import com.example.unipathapi.job.dto.response.EmployerJobStatsResponse;
import com.example.unipathapi.job.dto.response.JobApplicationStatsDto;
import com.example.unipathapi.job.dto.response.JobResponse;
import com.example.unipathapi.job.enums.JobType;
import com.example.unipathapi.job.enums.WorkMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobMediaRepository jobMediaRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private com.example.unipathapi.common.storage.FileStorageService fileStorageService;

    @Transactional
    public JobResponse createJob(Integer userId, JobRequest request) {
        List<CompanyMember> members = memberRepository.findByUserId(userId);
        if (members.isEmpty()) {
            throw new RuntimeException("Bạn chưa thuộc công ty nào trên hệ thống. Không thể đăng tin tuyển dụng.");
        }

        Company company;
        if (request.getCompanyId() != null) {
            CompanyMember member = memberRepository.findByCompanyIdAndUserId(request.getCompanyId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thuộc công ty này"));
            company = member.getCompany();
        } else if (members.size() == 1) {
            company = members.get(0).getCompany();
        } else {
            company = members.stream()
                    .filter(m -> "APPROVED".equalsIgnoreCase(m.getCompany().getStatus()))
                    .map(CompanyMember::getCompany)
                    .findFirst()
                    .orElse(members.get(0).getCompany());
        }

        if (!"APPROVED".equalsIgnoreCase(company.getStatus())) {
            throw new RuntimeException("Công ty của bạn chưa được System Admin duyệt");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản người đăng tin không tồn tại"));

        Job job = new Job();
        job.setCompany(company);
        job.setPostedBy(currentUser);
        job.setTitle(request.getTitle());
        job.setJobType(request.getJobType());
        job.setWorkMode(request.getWorkMode() != null ? request.getWorkMode() : WorkMode.ONSITE);
        job.setSalaryRange(request.getSalaryRange());
        job.setDescription(request.getDescription());
        job.setRequirements(normalizeRequirements(request.getRequirements()));
        job.setIsActive(false); // Mặc định chờ duyệt/kích hoạt

        if (request.getExpiredAt() != null) {
            if (request.getExpiredAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn ứng tuyển không được ở quá khứ");
            }
            job.setExpiredAt(request.getExpiredAt());
        }

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

        return buildJobResponse(savedJob, false);
    }

    public JobMediaResponse buildMediaResponse(JobMedia m) {
        if (m == null) return null;
        return JobMediaResponse.builder()
                .id(m.getId())
                .jobId(m.getJob() != null ? m.getJob().getId() : null)
                .fileUrl(m.getFileUrl())
                .fileType(m.getFileType())
                .mimeType(m.getMimeType())
                .fileSizeBytes(m.getFileSizeBytes())
                .displayOrder(m.getDisplayOrder())
                .uploadedAt(m.getUploadedAt())
                .build();
    }

    public JobResponse getJobDetail(Integer id) {
        return getJobDetail(id, null);
    }

    public JobResponse getJobDetail(Integer id, Integer currentUserId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));

        List<JobMedia> mediaList = jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(id);
        List<JobMediaResponse> mediaResponses = mediaList.stream()
                .map(this::buildMediaResponse)
                .collect(Collectors.toList());
        JobMediaResponse coverMedia = mediaResponses.isEmpty() ? null : mediaResponses.get(0);

        boolean isLiked = false;
        boolean isSaved = false;
        boolean hasApplied = false;

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            boolean isCandidate = currentUser != null && "CANDIDATE".equalsIgnoreCase(currentUser.getRole());
            if (isCandidate) {
                isLiked = jobLikeRepository.existsByJobIdAndUserId(id, currentUserId);
                isSaved = savedJobRepository.existsByCandidateIdAndJobId(currentUserId, id);
                hasApplied = applicationRepository.existsByJobIdAndCandidateId(id, currentUserId);
            }
        }

        JobResponse response = buildJobResponse(job, isLiked);
        response.setSaved(isSaved);
        response.setApplied(hasApplied);
        response.setMedia(mediaResponses);
        response.setCoverMedia(coverMedia);
        return response;
    }

    @Transactional
    public JobMediaResponse uploadJobMedia(Integer jobId, Integer userId, org.springframework.web.multipart.MultipartFile file) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        long existingCount = jobMediaRepository.countByJobId(jobId);
        if (existingCount >= 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi tin tuyển dụng chỉ được đính kèm tối đa 6 tệp đa phương tiện");
        }

        String storedUrl = null;
        try {
            storedUrl = fileStorageService.storeSingleJobMedia(file, "jobs/" + jobId);
            String extension = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1).toLowerCase()
                    : "";
            boolean isVideo = java.util.Arrays.asList("mp4", "webm").contains(extension);
            String fileType = isVideo ? "VIDEO" : "IMAGE";
            String mimeType = file.getContentType();

            JobMedia media = new JobMedia();
            media.setJob(job);
            media.setFileUrl(storedUrl);
            media.setFileType(fileType);
            media.setMimeType(mimeType);
            media.setFileSizeBytes((int) file.getSize());
            media.setDisplayOrder((int) existingCount);

            JobMedia saved = jobMediaRepository.save(media);
            return buildMediaResponse(saved);
        } catch (Exception e) {
            if (storedUrl != null) {
                fileStorageService.deleteFile(storedUrl);
            }
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Transactional
    public void deleteJobMedia(Integer mediaId, Integer userId) {
        JobMedia media = jobMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm"));

        validateEmployerCompanyAccess(media.getJob().getCompany().getId(), userId);

        String fileUrl = media.getFileUrl();
        jobMediaRepository.delete(media);
        if (fileUrl != null) {
            fileStorageService.deleteFile(fileUrl);
        }
    }

    @Transactional
    public List<JobMediaResponse> updateMediaOrder(Integer jobId, Integer userId, List<Integer> mediaIds) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        List<JobMedia> mediaList = jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(jobId);
        Map<Integer, JobMedia> mediaMap = mediaList.stream().collect(Collectors.toMap(JobMedia::getId, m -> m));

        for (int i = 0; i < mediaIds.size(); i++) {
            Integer mId = mediaIds.get(i);
            JobMedia m = mediaMap.get(mId);
            if (m != null) {
                m.setDisplayOrder(i);
                jobMediaRepository.save(m);
            }
        }

        return jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(jobId).stream()
                .map(this::buildMediaResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public JobResponse updateJob(Integer id, Integer userId, JobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getJobType() != null) job.setJobType(request.getJobType());
        if (request.getWorkMode() != null) job.setWorkMode(request.getWorkMode());
        if (request.getSalaryRange() != null) job.setSalaryRange(request.getSalaryRange());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getRequirements() != null) job.setRequirements(normalizeRequirements(request.getRequirements()));
        if (request.getExpiredAt() != null) {
            if (request.getExpiredAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn ứng tuyển không được ở quá khứ");
            }
            job.setExpiredAt(request.getExpiredAt());
        }

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
        return buildJobResponse(updatedJob, false);
    }

    public JobResponse closeJob(Integer id, Integer userId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        job.setIsActive(false);
        Job savedJob = jobRepository.save(job);
        return buildJobResponse(savedJob, false);
    }

    @Transactional
    public void deleteJob(Integer id, Integer userId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));

        validateEmployerCompanyAccess(job.getCompany().getId(), userId);

        long applicationsCount = applicationRepository.countByJobId(id);
        if (applicationsCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xoá tin tuyển dụng đã có người ứng tuyển. Vui lòng đóng tin.");
        }

        List<JobMedia> mediaList = jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(id);
        for (JobMedia m : mediaList) {
            if (m.getFileUrl() != null) {
                fileStorageService.deleteFile(m.getFileUrl());
            }
        }

        jobMediaRepository.deleteByJobId(id);
        jobSkillRepository.deleteByJobId(id);
        jobRepository.delete(job);
    }

    public List<JobResponse> getFeedJobs(Integer cursor, String keyword, Integer categoryId, Integer locationId, String jobType) {
        return getFeedJobs(cursor, keyword, categoryId, locationId, jobType, null, null);
    }

    public List<JobResponse> getFeedJobs(Integer cursor, String keyword, Integer categoryId, Integer locationId, String jobType, Integer currentUserId) {
        return getFeedJobs(cursor, keyword, categoryId, locationId, jobType, null, currentUserId);
    }

    public List<JobResponse> getFeedJobs(Integer cursor, String keyword, Integer categoryId, Integer locationId, String jobType, String workMode) {
        return getFeedJobs(cursor, keyword, categoryId, locationId, jobType, workMode, null);
    }

    public List<JobResponse> getFeedJobs(Integer cursor, String keyword, Integer categoryId, Integer locationId, String jobType, String workMode, Integer currentUserId) {
        Pageable pageable = PageRequest.of(0, 20);
        JobType parsedJobType = null;
        if (jobType != null && !jobType.isBlank()) {
            try {
                parsedJobType = JobType.valueOf(jobType.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        WorkMode parsedWorkMode = null;
        if (workMode != null && !workMode.isBlank()) {
            try {
                parsedWorkMode = WorkMode.valueOf(workMode.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        List<Job> jobs = jobRepository.findFeedJobs(cursor, keyword, categoryId, locationId, parsedJobType, parsedWorkMode, pageable);
        if (jobs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

        // 1. Batch check candidate interactions
        final Set<Integer> likedJobIds;
        final Set<Integer> savedJobIds;
        final Set<Integer> appliedJobIds;

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            boolean isCandidate = currentUser != null && "CANDIDATE".equalsIgnoreCase(currentUser.getRole());
            if (isCandidate) {
                likedJobIds = jobLikeRepository.findLikedJobIdsByUserIdAndJobIds(currentUserId, jobIds);
                savedJobIds = savedJobRepository.findSavedJobIdsByCandidateIdAndJobIds(currentUserId, jobIds);
                appliedJobIds = applicationRepository.findAppliedJobIdsByCandidateIdAndJobIds(currentUserId, jobIds);
            } else {
                likedJobIds = Collections.emptySet();
                savedJobIds = Collections.emptySet();
                appliedJobIds = Collections.emptySet();
            }
        } else {
            likedJobIds = Collections.emptySet();
            savedJobIds = Collections.emptySet();
            appliedJobIds = Collections.emptySet();
        }

        // 2. Batch fetch likes counts
        Map<Integer, Long> likesCountMap = jobLikeRepository.countLikesByJobIds(jobIds).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1], (v1, v2) -> v1));

        // 3. Batch fetch comments counts
        Map<Integer, Long> commentsCountMap = jobCommentRepository.countCommentsByJobIds(jobIds).stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1], (v1, v2) -> v1));

        // 4. Batch fetch application counts
        Map<Integer, Long> applicationsCountMap = applicationRepository.getStatsForJobIds(jobIds).stream()
                .collect(Collectors.toMap(JobApplicationStatsDto::getJobId, JobApplicationStatsDto::getTotalCount, (v1, v2) -> v1));

        // 5. Batch fetch skills
        Map<Integer, List<String>> skillsMap = jobSkillRepository.findByJobIdIn(jobIds).stream()
                .collect(Collectors.groupingBy(
                        js -> js.getJob().getId(),
                        Collectors.mapping(js -> js.getSkill().getName(), Collectors.toList())
                ));

        // 6. Batch fetch media
        Map<Integer, List<JobMediaResponse>> mediaMap = jobMediaRepository.findByJobIdIn(jobIds).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getJob().getId(),
                        Collectors.mapping(this::buildMediaResponse, Collectors.toList())
                ));

        // Assemble in memory - ZERO N+1 queries
        return jobs.stream().map(job -> {
            Integer jId = job.getId();
            List<JobMediaResponse> jobMedia = mediaMap.getOrDefault(jId, Collections.emptyList());
            JobMediaResponse cover = jobMedia.isEmpty() ? null : jobMedia.get(0);
            boolean isLiked = likedJobIds.contains(jId);

            return JobResponse.builder()
                    .id(jId)
                    .title(job.getTitle())
                    .jobType(job.getJobType())
                    .workMode(job.getWorkMode())
                    .salaryRange(job.getSalaryRange())
                    .description(job.getDescription())
                    .requirements(formatRequirementsForResponse(job.getRequirements()))
                    .categoryId(job.getCategory() != null ? job.getCategory().getId() : null)
                    .categoryName(job.getCategory() != null ? job.getCategory().getName() : null)
                    .locationId(job.getLocation() != null ? job.getLocation().getId() : null)
                    .locationAddress(job.getLocation() != null ? job.getLocation().getAddress() : null)
                    .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                    .companyName(job.getCompany() != null ? job.getCompany().getCompanyName() : null)
                    .companyScale(job.getCompany() != null ? job.getCompany().getCompanyScale() : null)
                    .companyLogoUrl(job.getCompany() != null ? job.getCompany().getLogoUrl() : null)
                    .skills(skillsMap.getOrDefault(jId, Collections.emptyList()))
                    .likesCount(likesCountMap.getOrDefault(jId, 0L))
                    .commentsCount(commentsCountMap.getOrDefault(jId, 0L))
                    .sharesCount(0L)
                    .applicationsCount(applicationsCountMap.getOrDefault(jId, 0L))
                    .liked(isLiked)
                    .saved(savedJobIds.contains(jId))
                    .applied(appliedJobIds.contains(jId))
                    .isActive(job.getIsActive())
                    .postedAt(job.getPostedAt())
                    .expiredAt(job.getExpiredAt())
                    .type("JOB")
                    .coverMedia(cover)
                    .media(jobMedia)
                    .build();
        }).collect(Collectors.toList());
    }

    public JobResponse buildJobResponse(Job job) {
        return buildJobResponse(job, false);
    }

    public JobResponse buildJobResponse(Job job, Boolean isLiked) {
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        List<String> skillNames = jobSkills.stream()
                .map(js -> js.getSkill().getName())
                .collect(Collectors.toList());

        long likesCount = jobLikeRepository.countByJobId(job.getId());
        long commentsCount = jobCommentRepository.countByJobId(job.getId());
        long sharesCount = jobShareRepository.countByJobId(job.getId());
        long applicationsCount = applicationRepository.countByJobId(job.getId());

        List<JobMedia> mediaList = jobMediaRepository.findByJobIdOrderByDisplayOrderAsc(job.getId());
        List<JobMediaResponse> mediaResponses = mediaList.stream()
                .map(this::buildMediaResponse)
                .collect(Collectors.toList());
        JobMediaResponse coverMedia = mediaResponses.isEmpty() ? null : mediaResponses.get(0);

        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .jobType(job.getJobType())
                .workMode(job.getWorkMode())
                .salaryRange(job.getSalaryRange())
                .description(job.getDescription())
                .requirements(formatRequirementsForResponse(job.getRequirements()))
                .categoryId(job.getCategory() != null ? job.getCategory().getId() : null)
                .categoryName(job.getCategory() != null ? job.getCategory().getName() : null)
                .locationId(job.getLocation() != null ? job.getLocation().getId() : null)
                .locationAddress(job.getLocation() != null ? job.getLocation().getAddress() : null)
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .companyName(job.getCompany() != null ? job.getCompany().getCompanyName() : null)
                .companyScale(job.getCompany() != null ? job.getCompany().getCompanyScale() : null)
                .companyLogoUrl(job.getCompany() != null ? job.getCompany().getLogoUrl() : null)
                .skills(skillNames)
                .likesCount(likesCount)
                .commentsCount(commentsCount)
                .sharesCount(sharesCount)
                .applicationsCount(applicationsCount)
                .liked(isLiked != null && isLiked)
                .saved(false)
                .applied(false)
                .isActive(job.getIsActive())
                .postedAt(job.getPostedAt())
                .expiredAt(job.getExpiredAt())
                .type("JOB")
                .coverMedia(coverMedia)
                .media(mediaResponses)
                .build();
    }

    private void validateEmployerCompanyAccess(Integer companyId, Integer userId) {
        Optional<CompanyMember> memberOpt = memberRepository.findByCompanyIdAndUserId(companyId, userId);
        if (memberOpt.isEmpty()) {
            throw new RuntimeException("Bạn không có quyền quản lý tin tuyển dụng của công ty này");
        }
    }

    public List<Integer> getManagedCompanyIds(Integer userId) {
        List<CompanyMember> memberships = memberRepository.findByUserId(userId);
        return memberships.stream()
                .filter(m -> "COMPANY_ADMIN".equalsIgnoreCase(m.getMemberRole()) || "RECRUITER".equalsIgnoreCase(m.getMemberRole()))
                .map(m -> m.getCompany().getId())
                .distinct()
                .collect(Collectors.toList());
    }

    public Page<EmployerJobManagementResponse> getEmployerJobs(Integer userId, String keyword, String status, String sort, int page, int size) {
        List<Integer> companyIds = getManagedCompanyIds(userId);
        if (companyIds.isEmpty()) {
            return Page.empty();
        }

        Sort sortObj = Sort.by(Sort.Direction.DESC, "postedAt", "id");
        if ("OLDEST".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "postedAt", "id");
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Job> jobsPage;
        if ("MOST_APPLICATIONS".equalsIgnoreCase(sort)) {
            jobsPage = jobRepository.searchEmployerJobsOrderByApplications(companyIds, keyword, status, PageRequest.of(page, size));
        } else {
            jobsPage = jobRepository.searchEmployerJobs(companyIds, keyword, status, pageable);
        }

        if (jobsPage.isEmpty()) {
            return jobsPage.map(job -> buildEmployerJobResponse(job, null));
        }

        // Batch fetch application metrics for all jobs on this page to prevent N+1
        List<Integer> jobIds = jobsPage.getContent().stream().map(Job::getId).collect(Collectors.toList());
        List<JobApplicationStatsDto> statsList = applicationRepository.getStatsForJobIds(jobIds);
        Map<Integer, JobApplicationStatsDto> statsMap = statsList.stream()
                .collect(Collectors.toMap(JobApplicationStatsDto::getJobId, s -> s));

        return jobsPage.map(job -> buildEmployerJobResponse(job, statsMap.get(job.getId())));
    }

    public EmployerJobStatsResponse getEmployerJobStats(Integer userId) {
        List<Integer> companyIds = getManagedCompanyIds(userId);
        if (companyIds.isEmpty()) {
            return EmployerJobStatsResponse.builder()
                    .totalJobs(0)
                    .activeJobs(0)
                    .closedJobs(0)
                    .totalApplications(0)
                    .build();
        }

        long totalJobs = jobRepository.countByCompanyIdIn(companyIds);
        long activeJobs = jobRepository.countActiveByCompanyIds(companyIds);
        long closedJobs = jobRepository.countClosedByCompanyIds(companyIds);
        long totalApplications = applicationRepository.countByCompanyIds(companyIds);

        return EmployerJobStatsResponse.builder()
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .closedJobs(closedJobs)
                .totalApplications(totalApplications)
                .build();
    }

    @Transactional
    public EmployerJobManagementResponse reopenJob(Integer id, Integer userId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tin tuyển dụng"));

        List<Integer> companyIds = getManagedCompanyIds(userId);
        if (!companyIds.contains(job.getCompany().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý tin tuyển dụng của công ty này");
        }

        if (job.getExpiredAt() != null && job.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tin tuyển dụng này đã hết hạn. Vui lòng chỉnh sửa và cập nhật hạn ứng tuyển mới trước khi mở lại.");
        }

        job.setIsActive(true);
        Job savedJob = jobRepository.save(job);

        List<JobApplicationStatsDto> statsList = applicationRepository.getStatsForJobIds(List.of(savedJob.getId()));
        JobApplicationStatsDto stats = statsList.isEmpty() ? null : statsList.get(0);

        return buildEmployerJobResponse(savedJob, stats);
    }

    public EmployerJobManagementResponse buildEmployerJobResponse(Job job, JobApplicationStatsDto stats) {
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        List<String> skillNames = jobSkills.stream()
                .map(js -> js.getSkill().getName())
                .collect(Collectors.toList());

        long totalApps = stats != null ? stats.getTotalCount() : applicationRepository.countByJobId(job.getId());
        long newApps = stats != null ? stats.getNewCount() : 0L;
        long interviewApps = stats != null ? stats.getInterviewCount() : 0L;

        return EmployerJobManagementResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .jobType(job.getJobType())
                .workMode(job.getWorkMode())
                .salaryRange(job.getSalaryRange())
                .description(job.getDescription())
                .requirements(formatRequirementsForResponse(job.getRequirements()))
                .categoryId(job.getCategory() != null ? job.getCategory().getId() : null)
                .categoryName(job.getCategory() != null ? job.getCategory().getName() : null)
                .locationId(job.getLocation() != null ? job.getLocation().getId() : null)
                .locationAddress(job.getLocation() != null ? job.getLocation().getAddress() : null)
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .companyName(job.getCompany() != null ? job.getCompany().getCompanyName() : null)
                .companyLogoUrl(job.getCompany() != null ? job.getCompany().getLogoUrl() : null)
                .skills(skillNames)
                .isActive(job.getIsActive())
                .createdAt(job.getCreatedAt())
                .postedAt(job.getPostedAt())
                .expiredAt(job.getExpiredAt())
                .applicationsCount(totalApps)
                .newApplicationsCount(newApps)
                .interviewApplicationsCount(interviewApps)
                .effectiveStatus(calculateEffectiveStatus(job.getIsActive(), job.getExpiredAt()))
                .build();
    }

    public static String calculateEffectiveStatus(Boolean isActive, LocalDateTime expiredAt) {
        if (expiredAt != null && expiredAt.isBefore(LocalDateTime.now())) {
            return "EXPIRED";
        }
        if (Boolean.TRUE.equals(isActive)) {
            return "ACTIVE";
        }
        return "CLOSED";
    }

    public static String normalizeRequirements(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]")) ||
            (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2)) {
            return trimmed;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String formatRequirementsForResponse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '\\' && i + 1 < inner.length()) {
                    char next = inner.charAt(++i);
                    switch (next) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (i + 4 < inner.length()) {
                                try {
                                    int codePoint = Integer.parseInt(inner.substring(i + 1, i + 5), 16);
                                    sb.append((char) codePoint);
                                    i += 4;
                                } catch (NumberFormatException e) {
                                    sb.append("\\u");
                                }
                            } else {
                                sb.append("\\u");
                            }
                            break;
                        default:
                            sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return trimmed;
    }
}


