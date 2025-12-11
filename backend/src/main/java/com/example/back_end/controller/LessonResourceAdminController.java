package com.example.back_end.controller;

import com.example.back_end.dto.LessonResourceDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.LessonResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/lesson-resources")
@PreAuthorize("hasRole('MANAGER')")
public class LessonResourceAdminController {

    private final LessonResourceService lessonResourceService;
    private final UserRepository userRepository;

    public LessonResourceAdminController(LessonResourceService lessonResourceService, UserRepository userRepository) {
        this.lessonResourceService = lessonResourceService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<LessonResourceDtos.ResourceResponse>> listResources(
            Authentication authentication,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long lessonId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String keyword
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(
                lessonResourceService.listForManager(user, courseId, lessonId, status, visibility, keyword)
        );
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(authentication.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Kh\u00F4ng t\u00ECm th\u1EA5y ng\u01B0\u1EDDi d\u00F9ng"));
    }
}
