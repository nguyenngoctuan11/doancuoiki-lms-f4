package com.example.back_end.controller;

import com.example.back_end.model.User;
import com.example.back_end.repository.CourseRepository;
import com.example.back_end.repository.UserRepository;
import com.example.back_end.repository.projection.TeacherCourseProjection;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses")
public class TeacherCourseQueryController {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public TeacherCourseQueryController(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<List<TeacherCourseProjection>> myCourses(Authentication auth) {
        String email = String.valueOf(auth.getPrincipal());
        return ResponseEntity.ok(courseRepository.findCoursesByCreatorEmail(email));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','MANAGER')")
    public ResponseEntity<List<TeacherCourseProjection>> instructorCourses(
            Authentication auth,
            @RequestParam(value = "status", required = false) String status
    ) {
        User user = userRepository.findByEmailIgnoreCase(String.valueOf(auth.getPrincipal())).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        boolean isManager = auth.getAuthorities().stream().anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()));
        if (isManager) {
            return ResponseEntity.ok(courseRepository.findAllCoursesForManager(status));
        }
        return ResponseEntity.ok(courseRepository.findCoursesByInstructor(user.getId(), status));
    }
}
