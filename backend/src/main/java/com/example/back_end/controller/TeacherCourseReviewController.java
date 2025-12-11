package com.example.back_end.controller;

import com.example.back_end.dto.CourseReviewDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.CourseReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/course-reviews")
@PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
public class TeacherCourseReviewController {

    private final CourseReviewService reviewService;
    private final UserRepository userRepository;

    public TeacherCourseReviewController(CourseReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<CourseReviewDtos.ReviewResponse>> list(
            Authentication authentication,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(reviewService.instructorModerationList(user, courseId, status));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CourseReviewDtos.ReviewResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody CourseReviewDtos.AdminUpdateRequest request
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(reviewService.updateStatusAs(user, id, request != null ? request.status : null,
                request != null ? request.adminNote : null));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(authentication.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
