package com.example.back_end.controller;

import com.example.back_end.dto.StudentDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.StudentProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/student/progress")
public class StudentProgressController {
    private final UserRepository userRepository;
    private final StudentProgressService studentProgressService;

    public StudentProgressController(UserRepository userRepository, StudentProgressService studentProgressService) {
        this.userRepository = userRepository;
        this.studentProgressService = studentProgressService;
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal())).orElseThrow();
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudentDtos.CourseProgress> courseProgress(@PathVariable Long courseId, Authentication auth) {
        User user = currentUser(auth);
        return ResponseEntity.ok(studentProgressService.getCourseProgress(user.getId(), courseId));
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> completeLesson(@PathVariable Long lessonId, Authentication auth) {
        User user = currentUser(auth);
        studentProgressService.markLessonCompleted(user.getId(), lessonId);
        return ResponseEntity.ok(Collections.singletonMap("ok", true));
    }
}
