package com.example.unipathapi.auth.service;

import com.example.unipathapi.auth.dto.request.CandidateRegisterRequest;
import com.example.unipathapi.auth.dto.request.EmployerRegisterRequest;
import com.example.unipathapi.auth.dto.request.ChangePasswordRequest;
import com.example.unipathapi.service.CompanyManagementService;

import com.example.unipathapi.auth.dto.request.AuthRequest;
import com.example.unipathapi.auth.dto.response.AuthResponse;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.entity.Company;
import com.example.unipathapi.user.repository.UserRepository;
import com.example.unipathapi.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private com.example.unipathapi.candidate.repository.CandidateProfileRepository candidateProfileRepository;

    @Autowired
    private com.example.unipathapi.employer.repository.EmployerProfileRepository employerProfileRepository;

    @Autowired
    private com.example.unipathapi.repository.CompanyRepository companyRepository;

    @Autowired
    private com.example.unipathapi.repository.CompanyJoinRequestRepository joinRequestRepository;

    @Autowired
    private CompanyManagementService companyService;

    @Transactional
    public String registerCandidate(CandidateRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(request.getPassword());
        user.setRole("CANDIDATE");
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        com.example.unipathapi.candidate.entity.CandidateProfile profile = new com.example.unipathapi.candidate.entity.CandidateProfile();
        profile.setUser(savedUser);
        profile.setFullName(request.getFullName().trim());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setUniversityName(request.getUniversityName());
        profile.setMajor(request.getMajor());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setSkills(request.getSkills());

        candidateProfileRepository.save(profile);
        return "Đăng ký tài khoản Ứng viên thành công!";
    }

    @Transactional
    public String registerEmployer(EmployerRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        // Validate company selection
        boolean hasExistingCompany = request.getCompanyId() != null;
        boolean hasNewCompany = request.getNewCompanyName() != null && !request.getNewCompanyName().trim().isEmpty();

        if (hasExistingCompany && hasNewCompany) {
            throw new RuntimeException("Không được đồng thời chọn công ty có sẵn và đề xuất công ty mới");
        }
        if (!hasExistingCompany && !hasNewCompany) {
            throw new RuntimeException("Vui lòng chọn một công ty đang làm việc hoặc nhập tên công ty mới");
        }

        // Create User
        User user = new User();
        user.setEmail(email);
        user.setPassword(request.getPassword());
        user.setRole("EMPLOYER");
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        // Create Employer Profile
        com.example.unipathapi.employer.entity.EmployerProfile profile = new com.example.unipathapi.employer.entity.EmployerProfile();
        profile.setUser(savedUser);
        profile.setFullName(request.getFullName().trim());
        profile.setPhone(request.getPhone());
        profile.setPosition(request.getPosition());
        profile.setBio(request.getBio());
        employerProfileRepository.save(profile);

        // Handle Company selection
        if (hasExistingCompany) {
            Company comp = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Công ty được chọn không tồn tại"));

            if (!"APPROVED".equalsIgnoreCase(comp.getStatus())) {
                throw new RuntimeException("Công ty này chưa được phê duyệt chính thức");
            }

            // Create CompanyJoinRequest with status PENDING
            com.example.unipathapi.entity.CompanyJoinRequest joinReq = new com.example.unipathapi.entity.CompanyJoinRequest();
            joinReq.setUser(savedUser);
            joinReq.setCompany(comp);
            joinReq.setStatus("PENDING");
            joinReq.setMessage("Đăng ký tài khoản Nhà tuyển dụng và xin gia nhập công ty");
            joinRequestRepository.save(joinReq);
        } else {
            // Propose new Company
            com.example.unipathapi.dto.request.CompanyProposeRequest propReq = new com.example.unipathapi.dto.request.CompanyProposeRequest();
            propReq.setCompanyName(request.getNewCompanyName());
            propReq.setContactEmail(request.getNewCompanyContactEmail());
            propReq.setTaxCode(request.getNewCompanyTaxCode());
            propReq.setCompanyScale(request.getNewCompanyScale() != null ? request.getNewCompanyScale() : "SME");
            propReq.setPhoneNumber(request.getNewCompanyPhoneNumber());
            propReq.setWebsite(request.getNewCompanyWebsite());
            propReq.setDescription(request.getNewCompanyDescription());
            propReq.setConfirmed(request.getConfirmed());

            com.example.unipathapi.dto.response.CompanyProposeResponse propRes = companyService.proposeCompany(savedUser.getId(), propReq);
            if (Boolean.TRUE.equals(propRes.getDuplicate())) {
                throw new RuntimeException("Công ty \"" + propRes.getCompanyName() + "\" đã tồn tại trong hệ thống. Vui lòng chọn công ty này từ danh sách.");
            }
            if (Boolean.TRUE.equals(propRes.getWarning()) && !Boolean.TRUE.equals(request.getConfirmed())) {
                throw new RuntimeException("CẢNH BÁO: Tên công ty tương tự đã tồn tại. Vui lòng xác nhận tiếp tục.");
            }
        }

        return "Đăng ký tài khoản Nhà tuyển dụng thành công!";
    }


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public String register(AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword()); // Lưu ý: Sau này cần mã hóa BCrypt ở đây
        newUser.setRole(request.getRole() != null ? request.getRole() : "CANDIDATE");

        userRepository.save(newUser);
        return "Đăng ký thành công.";
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng!"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng!");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khoá. Vui lòng liên hệ quản trị viên.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return new AuthResponse(token, String.valueOf(user.getId()), user.getRole(), "Đăng nhập thành công!");
    }

    @Transactional
    public String changePassword(Integer userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!user.getPassword().equals(request.getOldPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        user.setPassword(request.getNewPassword());
        userRepository.save(user);
        return "Đổi mật khẩu thành công!";
    }
}
