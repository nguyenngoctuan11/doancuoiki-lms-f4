package com.example.back_end.controller;

import com.example.back_end.dto.TeacherDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/teachers")
public class PublicTeacherController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    public ResponseEntity<List<TeacherDtos.TeacherHighlight>> list(
            @RequestParam(defaultValue = "12") int limit
    ) {
        return ResponseEntity.ok(fetchHighlights(limit));
    }

    @GetMapping("/highlights")
    public ResponseEntity<List<TeacherDtos.TeacherHighlight>> highlights(
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(fetchHighlights(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherDtos.TeacherProfile> detail(@PathVariable Long id) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT u.id, u.full_name, u.avatar_url, u.bio, u.username, u.email\n" +
                        "FROM dbo.users u\n" +
                        "JOIN dbo.user_roles ur ON ur.user_id = u.id\n" +
                        "JOIN dbo.roles r ON r.id = ur.role_id AND r.code = N'teacher'\n" +
                        "WHERE u.id = :id")
                .setParameter("id", id)
                .getResultList();
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Object[] row = rows.get(0);
        TeacherDtos.TeacherProfile profile = new TeacherDtos.TeacherProfile();
        profile.id = ((Number) row[0]).longValue();
        profile.fullName = row[1] != null ? row[1].toString() : "";
        profile.avatarUrl = row[2] != null ? row[2].toString() : null;
        profile.bio = row[3] != null ? row[3].toString() : "";
        profile.username = row[4] != null ? row[4].toString() : null;
        profile.email = row[5] != null ? row[5].toString() : null;

        @SuppressWarnings("unchecked")
        List<Object[]> courseRows = em.createNativeQuery(
                "SELECT c.id, c.title, c.slug, c.level, c.thumbnail_url, c.status\n" +
                        "FROM dbo.courses c\n" +
                        "WHERE c.created_by = :id AND c.status IN (N'published', N'active')\n" +
                        "ORDER BY c.created_at DESC")
                .setParameter("id", id)
                .getResultList();

        List<TeacherDtos.CourseSummary> courses = courseRows.stream().map(course -> {
            TeacherDtos.CourseSummary cs = new TeacherDtos.CourseSummary();
            cs.id = ((Number) course[0]).longValue();
            cs.title = course[1] != null ? course[1].toString() : "";
            cs.slug = course[2] != null ? course[2].toString() : null;
            cs.level = course[3] != null ? course[3].toString() : "";
            cs.thumbnailUrl = course[4] != null ? course[4].toString() : null;
            cs.status = course[5] != null ? course[5].toString() : "";
            return cs;
        }).collect(Collectors.toList());

        Long lessonCount = ((Number) em.createNativeQuery(
                "SELECT COUNT(l.id)\n" +
                        "FROM dbo.lessons l\n" +
                        "JOIN dbo.modules m ON m.id = l.module_id\n" +
                        "JOIN dbo.courses c ON c.id = m.course_id\n" +
                        "WHERE c.created_by = :id")
                .setParameter("id", id)
                .getSingleResult()).longValue();

        profile.courses = courses;
        profile.courseCount = courses.size();
        profile.lessonCount = lessonCount;
        profile.expertise = courses.stream()
                .map(cs -> cs.level == null ? "" : cs.level.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));

        return ResponseEntity.ok(profile);
    }

    private List<TeacherDtos.TeacherHighlight> fetchHighlights(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 24));
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT u.id, u.full_name, u.avatar_url, u.bio,\n" +
                        "       COUNT(DISTINCT c.id) AS course_count,\n" +
                        "       COUNT(l.id) AS lesson_count\n" +
                        "FROM dbo.users u\n" +
                        "JOIN dbo.user_roles ur ON ur.user_id = u.id\n" +
                        "JOIN dbo.roles r ON r.id = ur.role_id AND r.code = N'teacher'\n" +
                        "LEFT JOIN dbo.courses c ON c.created_by = u.id\n" +
                        "LEFT JOIN dbo.modules m ON m.course_id = c.id\n" +
                        "LEFT JOIN dbo.lessons l ON l.module_id = m.id\n" +
                        "GROUP BY u.id, u.full_name, u.avatar_url, u.bio\n" +
                        "ORDER BY lesson_count DESC, course_count DESC, u.full_name\n" +
                        "OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY")
                .setParameter("limit", safeLimit)
                .getResultList();

        return rows.stream().map(row -> {
            TeacherDtos.TeacherHighlight dto = new TeacherDtos.TeacherHighlight();
            dto.id = row[0] != null ? ((Number) row[0]).longValue() : null;
            dto.fullName = row[1] != null ? row[1].toString() : "Teacher";
            dto.avatarUrl = row[2] != null ? row[2].toString() : null;
            dto.bio = row[3] != null ? row[3].toString() : "";
            dto.courseCount = row[4] == null ? 0L : ((Number) row[4]).longValue();
            dto.lessonCount = row[5] == null ? 0L : ((Number) row[5]).longValue();
            return dto;
        }).collect(Collectors.toList());
    }
}
