package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.AuthRequest;
import com.example.unipathapi.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

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
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @Autowired
    private com.example.unipathapi.util.SecurityUtil securityUtil;

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@jakarta.validation.Valid @RequestBody com.example.unipathapi.dto.request.ChangePasswordRequest request,
                                            jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(authService.changePassword(userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
