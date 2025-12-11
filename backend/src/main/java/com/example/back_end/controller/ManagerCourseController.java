package com.example.back_end.controller;

import com.example.back_end.model.Course;
import com.example.back_end.model.User;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.example.back_end.repository.projection.ManagerPendingCourseProjection;

@RestController
@RequestMapping("/api/manager/courses")
public class ManagerCourseController {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public ManagerCourseController(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal())).orElseThrow();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ManagerPendingCourseProjection>> pending() {
        return ResponseEntity.ok(courseRepository.findPendingForManager());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> approve(@PathVariable Long id, Authentication auth) {
        Course c = courseRepository.findById(id).orElseThrow();
        c.setApprovalStatus("approved");
        c.setApprovedAt(LocalDateTime.now());
        c.setApprovedBy(currentUser(auth));
        c.setStatus("published");
        courseRepository.save(c);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, Authentication auth) {
        Course c = courseRepository.findById(id).orElseThrow();
        String note = body != null ? body.getOrDefault("note", null) : null;
        c.setApprovalStatus("rejected");
        c.setApprovalNote(note);
        c.setApprovedAt(LocalDateTime.now());
        c.setApprovedBy(currentUser(auth));
        // Optionally move course back to draft so it is not publicly visible
        if ("published".equalsIgnoreCase(c.getStatus())) {
            c.setStatus("draft");
        }
        courseRepository.save(c);
        return ResponseEntity.ok(Map.of("ok", true, "note", note));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!courseRepository.existsById(id)) return ResponseEntity.notFound().build();
        courseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
