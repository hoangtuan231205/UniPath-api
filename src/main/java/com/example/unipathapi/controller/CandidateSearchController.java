package com.example.unipathapi.controller;

import com.example.unipathapi.service.CandidateSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
public class CandidateSearchController {

    @Autowired
    private CandidateSearchService candidateSearchService;

    @GetMapping("/search")
    public ResponseEntity<?> searchCandidates(@RequestParam(required = false) String skill,
                                               @RequestParam(required = false) String major,
                                               @RequestParam(required = false) String university,
                                               @RequestParam(required = false) String keyword) {
        try {
            return ResponseEntity.ok(candidateSearchService.searchCandidates(skill, major, university, keyword));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
