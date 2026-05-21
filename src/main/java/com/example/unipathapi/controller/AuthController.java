package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.AuthRequest;
import com.example.unipathapi.dto.response.AuthResponse;
import com.example.unipathapi.entity.User;
import com.example.unipathapi.repository.UserRepository;
import com.example.unipathapi.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email đã tồn tại");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());
        newUser.setRole(request.getRole() != null ? request.getRole() : "STUDENT");

        userRepository.save(newUser);
        return ResponseEntity.ok().body("Đăng ký thành công.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            User user = userOpt.get();
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            AuthResponse response = new AuthResponse(token, String.valueOf(user.getId()), user.getRole(), "Đăng nhập thành công!");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body("Email hoặc mật khẩu không đúng!");
    }
}
