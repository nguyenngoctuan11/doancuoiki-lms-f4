package com.example.back_end.controller;

import com.example.back_end.dto.LessonNoteDtos;
import com.example.back_end.model.User;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.service.LessonNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lesson-notes")
public class LessonNoteController {
    private final LessonNoteService lessonNoteService;
    private final UserRepository userRepository;

    public LessonNoteController(LessonNoteService lessonNoteService, UserRepository userRepository) {
        this.lessonNoteService = lessonNoteService;
        this.userRepository = userRepository;
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonNoteDtos.NoteResponse>> listNotes(
            Authentication auth,
            @PathVariable Long courseId,
            @PathVariable Long lessonId
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(lessonNoteService.listNotes(user, courseId, lessonId));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LessonNoteDtos.NoteResponse> createNote(
            Authentication auth,
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestBody LessonNoteDtos.CreateRequest request
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(lessonNoteService.createNote(user, courseId, lessonId, request != null ? request.content : null));
    }

    @PostMapping("/{noteId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonNoteDtos.NoteResponse> replyNote(
            Authentication auth,
            @PathVariable Long noteId,
            @RequestBody LessonNoteDtos.CommentRequest request
    ) {
        User user = currentUser(auth);
        return ResponseEntity.ok(lessonNoteService.addComment(user, noteId, request != null ? request.content : null));
    }

    @PatchMapping("/{noteId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonNoteDtos.NoteResponse> updateStatus(
            Authentication auth,
            @PathVariable Long noteId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        User user = currentUser(auth);
        LessonNoteDtos.NoteResponse dto = lessonNoteService.getNoteForUser(noteId, user.getId());
        if (body != null && body.get("status") != null) {
            dto.status = body.get("status");
        }
        return ResponseEntity.ok(dto);
    }

    private User currentUser(Authentication auth) {
        if (auth == null) {
            throw new IllegalStateException("Chưa xác thực");
        }
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }
}
