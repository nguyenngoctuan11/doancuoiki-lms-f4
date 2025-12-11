package com.example.back_end.controller;

import com.example.back_end.dto.CourseReviewDtos;
import com.example.back_end.service.CourseReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/course-reviews")
@PreAuthorize("hasRole('MANAGER')")
public class CourseReviewAdminController {

    private final CourseReviewService reviewService;

    public CourseReviewAdminController(CourseReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<CourseReviewDtos.ReviewResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(reviewService.moderationList(status));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CourseReviewDtos.ReviewResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody CourseReviewDtos.AdminUpdateRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateStatus(id, request != null ? request.status : null,
                request != null ? request.adminNote : null));
    }
}
