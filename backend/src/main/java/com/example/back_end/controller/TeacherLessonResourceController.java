package com.example.back_end.controller;

import com.example.back_end.dto.LessonResourceDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.LessonResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/lesson-resources")
@PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
public class TeacherLessonResourceController {

    private final LessonResourceService resourceService;
    private final UserRepository userRepository;

    public TeacherLessonResourceController(LessonResourceService resourceService, UserRepository userRepository) {
        this.resourceService = resourceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public ResponseEntity<List<LessonResourceDtos.ResourceResponse>> list(
            Authentication authentication,
            @PathVariable Long courseId,
            @PathVariable Long lessonId
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(resourceService.listForTeacher(user, courseId, lessonId));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}")
    public ResponseEntity<LessonResourceDtos.ResourceResponse> create(
            Authentication authentication,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestBody LessonResourceDtos.CreateRequest request
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(resourceService.create(user, courseId, lessonId, request));
    }

    @PatchMapping("/{resourceId}")
    public ResponseEntity<LessonResourceDtos.ResourceResponse> update(
            Authentication authentication,
            @PathVariable Long resourceId,
            @RequestBody LessonResourceDtos.UpdateRequest request
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(resourceService.update(user, resourceId, request));
    }

    @PatchMapping("/{resourceId}/status")
    public ResponseEntity<LessonResourceDtos.ResourceResponse> changeStatus(
            Authentication authentication,
            @PathVariable Long resourceId,
            @RequestBody Map<String, String> body
    ) {
        User user = currentUser(authentication);
        String status = body != null ? body.get("status") : null;
        return ResponseEntity.ok(resourceService.changeStatus(user, resourceId, status));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<?> delete(
            Authentication authentication,
            @PathVariable Long resourceId
    ) {
        User user = currentUser(authentication);
        resourceService.delete(user, resourceId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null) throw new IllegalStateException("Y\u00EAu c\u1EA7u x\u00E1c th\u1EF1c");
        return userRepository.findByEmailIgnoreCase(String.valueOf(authentication.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Kh\u00F4ng t\u00ECm th\u1EA5y ng\u01B0\u1EDDi d\u00F9ng"));
    }
}
