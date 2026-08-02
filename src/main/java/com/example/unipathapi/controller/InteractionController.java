package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CommentRequest;
import com.example.unipathapi.service.InteractionService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private SecurityUtil securityUtil;

    // --- JOB LIKE ---
    @PostMapping("/api/jobs/{id}/like")
    public ResponseEntity<?> likeJob(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            interactionService.likeJob(id, userId);
            return ResponseEntity.ok("Đã thích tin tuyển dụng");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/jobs/{id}/like")
    public ResponseEntity<?> unlikeJob(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            interactionService.unlikeJob(id, userId);
            return ResponseEntity.ok("Đã bỏ thích tin tuyển dụng");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- POST LIKE ---
    @PostMapping("/api/posts/{id}/like")
    public ResponseEntity<?> likePost(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            interactionService.likePost(id, userId);
            return ResponseEntity.ok("Đã thích bài viết");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/posts/{id}/like")
    public ResponseEntity<?> unlikePost(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            interactionService.unlikePost(id, userId);
            return ResponseEntity.ok("Đã bỏ thích bài viết");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- JOB COMMENTS ---
    @GetMapping("/api/jobs/{id}/comments")
    public ResponseEntity<?> getJobComments(@PathVariable Integer id, @RequestParam(required = false) Integer cursor) {
        try {
            return ResponseEntity.ok(interactionService.getJobComments(id, cursor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/jobs/{id}/comments")
    public ResponseEntity<?> createJobComment(@PathVariable Integer id,
                                             @Valid @RequestBody CommentRequest commentRequest,
                                             HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(interactionService.createJobComment(id, userId, commentRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<?> deleteJobComment(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            interactionService.deleteJobComment(id, userId);
            return ResponseEntity.ok("Xoá bình luận thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- POST COMMENTS ---
    @GetMapping("/api/posts/{id}/comments")
    public ResponseEntity<?> getPostComments(@PathVariable Integer id, @RequestParam(required = false) Integer cursor) {
        try {
            return ResponseEntity.ok(interactionService.getPostComments(id, cursor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/posts/{id}/comments")
    public ResponseEntity<?> createPostComment(@PathVariable Integer id,
                                              @Valid @RequestBody CommentRequest commentRequest,
                                              HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            return ResponseEntity.ok(interactionService.createPostComment(id, userId, commentRequest));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/posts/comments/{id}")
    public ResponseEntity<?> deletePostComment(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            interactionService.deletePostComment(id, userId);
            return ResponseEntity.ok("Xoá bình luận thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- JOB SHARE ---
    @PostMapping("/api/jobs/{id}/share")
    public ResponseEntity<?> shareJob(@PathVariable Integer id, HttpServletRequest request) {
        try {
            Integer userId = securityUtil.getCurrentUserId(request);
            interactionService.shareJob(id, userId);
            return ResponseEntity.ok("Đã chia sẻ tin tuyển dụng");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
