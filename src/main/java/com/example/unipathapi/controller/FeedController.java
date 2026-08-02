package com.example.unipathapi.controller;

import com.example.unipathapi.service.CommunityPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed")
@CrossOrigin(origins = "*")
public class FeedController {

    @Autowired
    private CommunityPostService postService;

    @GetMapping
    public ResponseEntity<?> getMergedFeed(@RequestParam(required = false) Integer cursor) {
        try {
            return ResponseEntity.ok(postService.getMergedFeed(cursor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
