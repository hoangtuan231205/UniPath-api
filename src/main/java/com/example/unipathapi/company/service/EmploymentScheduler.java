package com.example.unipathapi.company.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmploymentScheduler {

    @Autowired
    private EmploymentInvitationService invitationService;

    // Chạy vào 00:05 mỗi ngày theo giờ Việt Nam để tự động kích hoạt các hợp đồng nhân sự PENDING_START đến ngày bắt đầu
    @Scheduled(cron = "0 5 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void scheduleActivateDueEmployments() {
        invitationService.activateDueEmployments();
    }

    // Tự động kích hoạt các hợp đồng PENDING_START khi ứng dụng khởi động lại sau thời gian ngừng hoạt động
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        invitationService.activateDueEmployments();
    }
}
