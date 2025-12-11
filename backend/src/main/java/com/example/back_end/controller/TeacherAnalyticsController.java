package com.example.back_end.controller;

import com.example.back_end.dto.TeacherAnalyticsDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.TeacherAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/analytics")
public class TeacherAnalyticsController {

    private final TeacherAnalyticsService analyticsService;
    private final UserRepository userRepository;

    public TeacherAnalyticsController(TeacherAnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<TeacherAnalyticsDtos.DashboardResponse> dashboard(
            Authentication auth,
            @RequestParam(value = "days", required = false, defaultValue = "30") Integer days
    ) {
        User user = currentUser(auth);
        int window = sanitizeDays(days);
        TeacherAnalyticsDtos.DashboardResponse data = analyticsService.buildDashboard(user, window);
        return ResponseEntity.ok(data);
    }

    private int sanitizeDays(Integer days) {
        int value = days != null ? days : 30;
        if (value < 7) value = 7;
        if (value > 90) value = 90;
        return value;
    }

    private User currentUser(Authentication auth) {
        if (auth == null) {
            throw new IllegalStateException("Chưa xác thực");
        }
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
