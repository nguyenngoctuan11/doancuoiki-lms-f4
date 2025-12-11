package com.example.back_end.controller;

import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.LessonNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/lesson-notes")
public class TeacherLessonNoteController {
    private final LessonNoteService lessonNoteService;
    private final UserRepository userRepository;

    public TeacherLessonNoteController(LessonNoteService lessonNoteService, UserRepository userRepository) {
        this.lessonNoteService = lessonNoteService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<Map<String, Object>> listForInstructor(
            Authentication auth,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String query
    ) {
        User user = currentUser(auth);
        var data = lessonNoteService.listNotesForTeacher(user, courseId, status, query);
        return ResponseEntity.ok(Map.of("data", data));
    }

    private User currentUser(Authentication auth) {
        if (auth == null) {
            throw new IllegalStateException("Chưa xác thực");
        }
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
