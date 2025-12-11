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
@RequestMapping("/api/course-reviews")
public class CourseReviewController {

    private final CourseReviewService reviewService;
    private final UserRepository userRepository;

    public CourseReviewController(CourseReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseReviewDtos.ReviewResponse>> listCourseReviews(
            @PathVariable Long courseId,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(reviewService.listApprovedReviews(courseId, limit));
    }

    @GetMapping("/course/{courseId}/summary")
    public ResponseEntity<CourseReviewDtos.SummaryResponse> courseSummary(@PathVariable Long courseId) {
        return ResponseEntity.ok(reviewService.courseSummary(courseId));
    }

    @GetMapping("/instructor/{instructorId}/summary")
    public ResponseEntity<CourseReviewDtos.SummaryResponse> instructorSummary(@PathVariable Long instructorId) {
        return ResponseEntity.ok(reviewService.instructorSummary(instructorId));
    }

    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CourseReviewDtos.ReviewResponse> createOrUpdate(
            Authentication authentication,
            @PathVariable Long courseId,
            @RequestBody CourseReviewDtos.CreateRequest request
    ) {
        User student = currentUser(authentication);
        return ResponseEntity.ok(reviewService.createOrUpdateReview(student, courseId, request));
    }

    @GetMapping("/course/{courseId}/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CourseReviewDtos.ReviewResponse> myReview(
            Authentication authentication,
            @PathVariable Long courseId
    ) {
        User student = currentUser(authentication);
        return ResponseEntity.ok(reviewService.findReviewOfStudent(student, courseId));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(authentication.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
