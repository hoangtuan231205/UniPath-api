package com.example.unipathapi.controller;

import com.example.unipathapi.service.CommunityPostService;
import com.example.unipathapi.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@CrossOrigin(origins = "*")
public class FeedController {

    @Autowired
    private CommunityPostService postService;

    @Autowired
    private SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<?> getMergedFeed(@RequestParam(required = false) Integer cursor, HttpServletRequest request) {
        try {
            Integer currentUserId = securityUtil.getOptionalCurrentUserId(request);
            return ResponseEntity.ok(postService.getMergedFeed(cursor, currentUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
