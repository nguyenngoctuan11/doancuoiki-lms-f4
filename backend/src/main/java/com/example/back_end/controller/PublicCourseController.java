package com.example.back_end.controller;

import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.projection.CourseCardProjection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/courses")
public class PublicCourseController {
    private final CourseRepository courseRepository;

    public PublicCourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public ResponseEntity<List<CourseCardProjection>> list(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(value = "q", required = false) String keyword
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String normalizedKeyword = normalizeKeyword(keyword);
        return ResponseEntity.ok(courseRepository.findCourseCards(status, offset, safeLimit, normalizedKeyword));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
