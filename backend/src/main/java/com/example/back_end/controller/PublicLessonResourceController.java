package com.example.back_end.controller;

import com.example.back_end.dto.LessonResourceDtos;
import com.example.back_end.service.LessonResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/courses")
public class PublicLessonResourceController {

    private final LessonResourceService resourceService;

    public PublicLessonResourceController(LessonResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/{courseId}/resources")
    public ResponseEntity<List<LessonResourceDtos.ResourceResponse>> listPublicResources(
            @PathVariable Long courseId,
            @RequestParam(value = "lessonId", required = false) Long lessonId
    ) {
        return ResponseEntity.ok(resourceService.listPublicResources(courseId, lessonId));
    }

    @GetMapping("/resources")
    public ResponseEntity<List<LessonResourceDtos.ResourceResponse>> listPublicResourcesByQuery(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "lessonId", required = false) Long lessonId
    ) {
        return ResponseEntity.ok(resourceService.listPublicResources(courseId, lessonId));
    }
}
