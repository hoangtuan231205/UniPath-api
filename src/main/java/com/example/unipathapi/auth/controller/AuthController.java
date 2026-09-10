package com.example.unipathapi.auth.controller;

import com.example.unipathapi.auth.dto.request.AuthRequest;
import com.example.unipathapi.auth.dto.request.CandidateRegisterRequest;
import com.example.unipathapi.auth.dto.request.ChangePasswordRequest;
import com.example.unipathapi.auth.dto.request.EmployerRegisterRequest;
import com.example.unipathapi.auth.service.AuthService;
import com.example.unipathapi.common.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

        @PostMapping("/register/candidate")
    public ResponseEntity<?> registerCandidate(@Valid @RequestBody CandidateRegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.registerCandidate(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/employer")
    public ResponseEntity<?> registerEmployer(@Valid @RequestBody EmployerRegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.registerEmployer(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            String message = authService.register(request);
            return ResponseEntity.ok().body(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("khoá") || e.getMessage().contains("khóa"))) {
                return ResponseEntity.status(403).body(e.getMessage());
            }
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(authService.changePassword(userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
