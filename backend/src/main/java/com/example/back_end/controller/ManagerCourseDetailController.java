package com.example.back_end.controller;

import com.example.back_end.dto.public_.PublicCourseDetailDto;
import com.example.back_end.service.PublicCourseQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/courses")
public class ManagerCourseDetailController {
    private final PublicCourseQueryService queryService;

    public ManagerCourseDetailController(PublicCourseQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{id}/detail-sql")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        PublicCourseDetailDto dto = queryService.loadCourseDetailById(id, true);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }
}

