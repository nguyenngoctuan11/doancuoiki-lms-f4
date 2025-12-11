package com.example.back_end.controller;

import com.example.back_end.dto.LessonResourceDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.LessonResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lesson-resources")
public class LessonResourceController {

    private final LessonResourceService resourceService;
    private final UserRepository userRepository;

    public LessonResourceController(LessonResourceService resourceService, UserRepository userRepository) {
        this.resourceService = resourceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonResourceDtos.ResourceResponse>> listResources(
            Authentication authentication,
            @PathVariable Long courseId,
            @PathVariable Long lessonId
    ) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(resourceService.listForStudent(user, courseId, lessonId));
    }

    @GetMapping("/{resourceId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> download(
            Authentication authentication,
            @PathVariable Long resourceId
    ) {
        User user = currentUser(authentication);
        LessonResourceDtos.DownloadPayload payload = resourceService.prepareDownload(user, resourceId);
        return ResponseEntity.ok(Map.of(
                "redirectUrl", payload.url,
                "type", payload.redirect ? "redirect" : "file"
        ));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null) throw new IllegalStateException("Y\u00EAu c\u1EA7u x\u00E1c th\u1EF1c");
        return userRepository.findByEmailIgnoreCase(String.valueOf(authentication.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Kh\u00F4ng t\u00ECm th\u1EA5y ng\u01B0\u1EDDi d\u00F9ng"));
    }
}
