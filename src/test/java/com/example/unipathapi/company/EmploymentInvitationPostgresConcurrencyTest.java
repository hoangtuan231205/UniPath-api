package com.example.unipathapi.company;

import com.example.unipathapi.company.entity.Company;
import com.example.unipathapi.company.entity.EmploymentInvitation;
import com.example.unipathapi.company.repository.CompanyRepository;
import com.example.unipathapi.company.repository.EmploymentInvitationRepository;
import com.example.unipathapi.user.entity.User;
import com.example.unipathapi.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EmploymentInvitationPostgresConcurrencyTest {

    @Autowired
    private EmploymentInvitationRepository invitationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    private EmploymentInvitation testInvitation;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.findAll().stream().findFirst().orElse(null);
        User candidate = userRepository.findAll().stream().filter(u -> "CANDIDATE".equalsIgnoreCase(u.getRole())).findFirst().orElse(null);
        User employer = userRepository.findAll().stream().filter(u -> !"CANDIDATE".equalsIgnoreCase(u.getRole())).findFirst().orElse(candidate);

        if (company != null && candidate != null && employer != null) {
            EmploymentInvitation inv = new EmploymentInvitation();
            inv.setCompany(company);
            inv.setCandidate(candidate);
            inv.setInvitedBy(employer);
            inv.setInvitationType("DIRECT");
            inv.setStatus("PENDING");
            inv.setPosition("Nhân viên kiểm thử đồng thời");
            inv.setSalaryType("HOURLY");
            inv.setBaseSalaryPerHour(new BigDecimal("35000"));
            inv.setStartDate(LocalDate.now().plusDays(5));
            inv.setExpiresAt(LocalDateTime.now().plusDays(3));
            testInvitation = invitationRepository.save(inv);
        }
    }

    @AfterEach
    void tearDown() {
        if (testInvitation != null && testInvitation.getId() != null) {
            invitationRepository.deleteById(testInvitation.getId());
        }
    }

    @Test
    @DisplayName("PostgreSQL Concurrency Test: 2 thao tác ACCEPT đồng thời trên cùng lời mời PENDING -> Đúng 1 thành công (1), 1 thất bại (0)")
    void testConcurrentAcceptAtomic_ExactlyOneSucceeds() throws Exception {
        if (testInvitation == null) return;

        int threadsCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch readyLatch = new CountDownLatch(threadsCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Chờ kích hoạt đồng thời
                    int rows = invitationRepository.acceptAtomic(
                            testInvitation.getId(),
                            testInvitation.getCandidate().getId(),
                            LocalDateTime.now()
                    );
                    if (rows == 1) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Bắn 2 luồng cùng lúc

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished);

        // Đảm bảo nguyên tử: Đúng 1 thread cập nhật thành công, 1 thread nhận 0 rows
        assertEquals(1, successCount.get(), "Chỉ đúng 1 thao tác được cập nhật trạng thái thành công");
        assertEquals(1, failCount.get(), "Thao tác còn lại bị chặn do điều kiện conditional UPDATE không còn khớp PENDING");

        // Kiểm tra trạng thái thực tế trong database PostgreSQL
        EmploymentInvitation updatedDb = invitationRepository.findById(testInvitation.getId()).orElseThrow();
        assertEquals("ACCEPTED", updatedDb.getStatus());
        assertNotNull(updatedDb.getRespondedAt());
    }

    @Test
    @DisplayName("PostgreSQL Atomic Expiration: expireAtomic cập nhật đúng bản ghi quá hạn và trả về 1")
    void testExpireAtomic_ExpiredRecord_Succeeds() {
        if (testInvitation == null) return;

        // Cập nhật expiresAt vào quá khứ
        testInvitation.setExpiresAt(LocalDateTime.now().minusHours(2));
        testInvitation = invitationRepository.save(testInvitation);

        LocalDateTime now = LocalDateTime.now();
        int affected = invitationRepository.expireAtomic(testInvitation.getId(), now);

        assertEquals(1, affected, "expireAtomic phải cập nhật thành công bản ghi PENDING đã quá hạn");

        EmploymentInvitation inDb = invitationRepository.findById(testInvitation.getId()).orElseThrow();
        assertEquals("EXPIRED", inDb.getStatus());
    }
}
