package com.example.unipathapi.candidate.controller;

import com.example.unipathapi.candidate.service.CandidateSearchService;
import com.example.unipathapi.common.security.SecurityUtil;
import com.example.unipathapi.company.repository.CompanyMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/api/candidates")
@CrossOrigin(origins = "*")
public class CandidateSearchController {

    @Autowired
    private CandidateSearchService candidateSearchService;

    @Autowired
    private CompanyMemberRepository memberRepository;

    @Autowired
    private SecurityUtil securityUtil;

    // Rate limiter: max 30 requests per 60 seconds per user
    private final Map<Integer, ConcurrentLinkedQueue<Long>> userRequestTimestamps = new ConcurrentHashMap<>();

    @GetMapping("/search")
    public ResponseEntity<?> searchCandidates(@RequestParam(required = false) Integer companyId,
                                               @RequestParam(required = false) String skill,
                                               @RequestParam(required = false) String major,
                                               @RequestParam(required = false) String university,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false, defaultValue = "0") int page,
                                               @RequestParam(required = false, defaultValue = "10") int size,
                                               HttpServletRequest request) {
        try {
            Integer currentUserId = securityUtil.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Yêu cầu xác thực tài khoản (JWT)");
            }

            // 1. Mandatory companyId for Direct Invitation search
            if (companyId == null) {
                return ResponseEntity.badRequest().body("Yêu cầu mã công ty (companyId) hợp lệ để thực hiện tìm kiếm ứng viên");
            }

            // 2. Mandatory COMPANY_ADMIN authorization check
            boolean isCompanyAdmin = memberRepository.existsByCompanyIdAndUserIdAndMemberRole(companyId, currentUserId, "COMPANY_ADMIN");
            if (!isCompanyAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ Quản trị viên công ty (COMPANY_ADMIN) của doanh nghiệp này mới có quyền tìm kiếm ứng viên");
            }

            // 3. Rate limiting (max 30 req/min)
            if (!checkRateLimit(currentUserId)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Bạn đã thực hiện quá nhiều lượt tìm kiếm. Vui lòng thử lại sau một phút.");
            }

            return ResponseEntity.ok(candidateSearchService.searchCandidates(skill, major, university, keyword, page, size));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Lỗi tìm kiếm ứng viên";
            if (msg.startsWith("403:")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg.substring(4).trim());
            }
            if (msg.startsWith("401:")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg.substring(4).trim());
            }
            return ResponseEntity.badRequest().body(msg);
        }
    }

    private boolean checkRateLimit(Integer userId) {
        long now = System.currentTimeMillis();
        long windowMillis = 60_000L; // 60 seconds
        ConcurrentLinkedQueue<Long> queue = userRequestTimestamps.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        while (!queue.isEmpty() && now - queue.peek() > windowMillis) {
            queue.poll();
        }
        if (queue.size() >= 30) {
            return false;
        }
        queue.add(now);
        return true;
    }
}
