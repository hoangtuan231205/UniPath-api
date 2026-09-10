package com.example.unipathapi.controller;

import com.example.unipathapi.dto.request.CommunityPostRequest;
import com.example.unipathapi.service.CommunityPostService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class CommunityPostController {

    @Autowired
    private CommunityPostService postService;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<?> createPost(@Valid @RequestBody CommunityPostRequest request, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(postService.createPost(userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{postId}/media")
    public ResponseEntity<?> uploadPostMedia(@PathVariable Integer postId,
                                            @RequestParam("files") List<MultipartFile> files,
                                            HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(postService.uploadPostMedia(postId, userId, files));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<?> deletePostMedia(@PathVariable Integer mediaId, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            postService.deletePostMedia(mediaId, userId);
            return ResponseEntity.ok("Xoá tệp đính kèm thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyPosts(@RequestParam(required = false) Integer cursor, HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(postService.getUserPosts(currentUserId, cursor, currentUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPosts(@PathVariable Integer userId,
                                          @RequestParam(required = false) Integer cursor,
                                          HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getOptionalCurrentUserId(httpRequest);
            return ResponseEntity.ok(postService.getUserPosts(userId, cursor, currentUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostDetail(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer currentUserId = securityUtil.getOptionalCurrentUserId(httpRequest);
            return ResponseEntity.ok(postService.getPostDetail(id, currentUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Integer id,
                                        @Valid @RequestBody CommunityPostRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            return ResponseEntity.ok(postService.updatePost(id, userId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Integer id, HttpServletRequest httpRequest) {
        try {
            Integer userId = securityUtil.getCurrentUserId(httpRequest);
            postService.deletePost(id, userId);
            return ResponseEntity.ok("Xoá bài viết thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
