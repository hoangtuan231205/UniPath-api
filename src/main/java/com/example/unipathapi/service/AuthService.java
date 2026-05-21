package com.example.unipathapi.service;

import com.example.unipathapi.dto.request.AuthRequest;
import com.example.unipathapi.dto.response.AuthResponse;
import com.example.unipathapi.entity.User;
import com.example.unipathapi.repository.UserRepository;
import com.example.unipathapi.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

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

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return new AuthResponse(token, String.valueOf(user.getId()), user.getRole(), "Đăng nhập thành công!");
    }
}
