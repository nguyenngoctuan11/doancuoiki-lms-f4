package com.example.back_end.controller;

import com.example.back_end.dto.public_.PublicCourseDetailDto;
import com.example.back_end.service.PublicCourseQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/courses")
public class PublicCourseDetailController {
    private final PublicCourseQueryService queryService;

    public PublicCourseDetailController(PublicCourseQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{slug}/detail-sql")
    public ResponseEntity<?> detail(@PathVariable String slug) {
        PublicCourseDetailDto dto = queryService.loadCourseDetailBySlug(slug);
        if (dto == null && slug.matches("\\d+")) {
            dto = queryService.loadCourseDetailById(Long.parseLong(slug), false);
        }
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }
}

