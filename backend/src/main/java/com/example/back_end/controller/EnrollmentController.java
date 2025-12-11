package com.example.back_end.controller;

import com.example.back_end.model.Course;
import com.example.back_end.model.User;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class EnrollmentController {
    @PersistenceContext private EntityManager em;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    public EnrollmentController(UserRepository userRepository, CourseRepository courseRepository){
        this.userRepository = userRepository; this.courseRepository = courseRepository;
    }

    private User currentUser(Authentication auth){ return userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal())).orElseThrow(); }

    @PostMapping("/{courseId}/enroll")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> enroll(@PathVariable Long courseId, Authentication auth){
        Course c = courseRepository.findById(courseId).orElseThrow();
        Long uid = currentUser(auth).getId();
        // Try insert; if unique constraint hit, treat as already enrolled
        int n = em.createNativeQuery(
                "INSERT INTO dbo.enrollments(user_id, course_id, source, status)\n" +
                "SELECT :uid, :cid, N'free', N'active'\n" +
                "WHERE NOT EXISTS (SELECT 1 FROM dbo.enrollments WHERE user_id = :uid AND course_id = :cid)")
                .setParameter("uid", uid)
                .setParameter("cid", c.getId())
                .executeUpdate();
        boolean existed = (n == 0);
        return ResponseEntity.ok(Map.of("ok", true, "enrolled", true, "existed", existed));
    }
}
