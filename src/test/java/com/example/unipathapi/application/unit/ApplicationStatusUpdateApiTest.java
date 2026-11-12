package com.example.unipathapi.application.unit;

import com.example.unipathapi.application.controller.ApplicationController;
import com.example.unipathapi.application.dto.request.UpdateApplicationStatusRequest;
import com.example.unipathapi.application.dto.response.ApplicationResponse;
import com.example.unipathapi.application.service.ApplicationService;
import com.example.unipathapi.common.exception.GlobalExceptionHandler;
import com.example.unipathapi.common.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ApplicationStatusUpdateApiTest {

    private MockMvc mockMvc;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ApplicationController applicationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(applicationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PATCH /api/applications/18/status without body returns 400 Bad Request with missing/invalid format message")
    void testUpdateStatus_MissingRequestBody_Returns400() throws Exception {
        mockMvc.perform(patch("/api/applications/18/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Dữ liệu gửi lên không đúng định dạng hoặc giá trị không hợp lệ")));

        verify(applicationService, never()).updateApplicationStatus(any(), any(), any());
    }

    @Test
    @DisplayName("PATCH /api/applications/18/status with valid INTERVIEW JSON payload succeeds")
    void testUpdateStatus_ValidInterviewPayload_Success() throws Exception {
        when(securityUtil.getCurrentUserId(any(HttpServletRequest.class))).thenReturn(10);

        ApplicationResponse mockResponse = new ApplicationResponse();
        mockResponse.setId(18);
        mockResponse.setStatus("INTERVIEW");
        mockResponse.setInterviewAt(LocalDateTime.of(2026, 10, 15, 9, 0, 0));
        mockResponse.setInterviewLocation("Phòng họp 302");
        mockResponse.setNote("Phỏng vấn vòng 1");

        when(applicationService.updateApplicationStatus(eq(18), eq(10), any(UpdateApplicationStatusRequest.class)))
                .thenReturn(mockResponse);

        String jsonPayload = "{\"status\":\"INTERVIEW\",\"interviewAt\":\"2026-10-15T09:00:00\",\"interviewLocation\":\"Phòng họp 302\",\"note\":\"Phỏng vấn vòng 1\"}";

        mockMvc.perform(patch("/api/applications/18/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(18)))
                .andExpect(jsonPath("$.status", is("INTERVIEW")))
                .andExpect(jsonPath("$.interviewLocation", is("Phòng họp 302")));

        verify(applicationService, times(1)).updateApplicationStatus(eq(18), eq(10), any(UpdateApplicationStatusRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/applications/18/status with valid REVIEWING JSON payload succeeds")
    void testUpdateStatus_ValidReviewingPayload_Success() throws Exception {
        when(securityUtil.getCurrentUserId(any(HttpServletRequest.class))).thenReturn(10);

        ApplicationResponse mockResponse = new ApplicationResponse();
        mockResponse.setId(18);
        mockResponse.setStatus("REVIEWING");
        mockResponse.setNote("Đang duyệt CV");

        when(applicationService.updateApplicationStatus(eq(18), eq(10), any(UpdateApplicationStatusRequest.class)))
                .thenReturn(mockResponse);

        String jsonPayload = "{\"status\":\"REVIEWING\",\"interviewAt\":null,\"interviewLocation\":null,\"note\":\"Đang duyệt CV\"}";

        mockMvc.perform(patch("/api/applications/18/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(18)))
                .andExpect(jsonPath("$.status", is("REVIEWING")));

        verify(applicationService, times(1)).updateApplicationStatus(eq(18), eq(10), any(UpdateApplicationStatusRequest.class));
    }
}
