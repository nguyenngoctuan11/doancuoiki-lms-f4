package com.example.back_end.service;

import com.example.back_end.dto.StudentDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentProgressService {
    @PersistenceContext private EntityManager em;

    @Transactional(readOnly = true)
    public StudentDtos.CourseProgress getCourseProgress(Long userId, Long courseId) {
        Object[] courseRow = (Object[]) em.createNativeQuery(
                        "SELECT c.slug, c.title FROM dbo.courses c WHERE c.id = :cid")
                .setParameter("cid", courseId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (courseRow == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        Number totalLessonsNum = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.lessons l JOIN dbo.modules m ON l.module_id = m.id WHERE m.course_id = :cid")
                .setParameter("cid", courseId)
                .getSingleResult();
        int totalLessons = totalLessonsNum != null ? totalLessonsNum.intValue() : 0;

        @SuppressWarnings("unchecked")
        List<Number> completedLessonIds = (List<Number>) em.createNativeQuery(
                        "SELECT lp.lesson_id FROM dbo.lesson_progress lp " +
                                "JOIN dbo.lessons l ON l.id = lp.lesson_id " +
                                "JOIN dbo.modules m ON m.id = l.module_id " +
                                "WHERE lp.user_id = :uid AND m.course_id = :cid " +
                                "AND (lp.completed_at IS NOT NULL OR lp.progress_percent >= 95)")
                .setParameter("uid", userId)
                .setParameter("cid", courseId)
                .getResultList();

        int completedLessons = completedLessonIds.size();
        double completionPercent = totalLessons == 0 ? 0 : Math.round((completedLessons * 100.0) / totalLessons);

        StudentDtos.CourseProgress progress = new StudentDtos.CourseProgress();
        progress.courseId = courseId;
        progress.courseSlug = courseRow[0] != null ? courseRow[0].toString() : null;
        progress.totalLessons = totalLessons;
        progress.completedLessons = completedLessons;
        progress.completionPercent = completionPercent;
        progress.completedLessonIds = completedLessonIds.stream().map(Number::longValue).collect(Collectors.toList());
        return progress;
    }

    @Transactional
    public void markLessonCompleted(Long userId, Long lessonId) {
        Number exists = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dbo.lessons WHERE id = :lid")
                .setParameter("lid", lessonId)
                .getSingleResult();
        if (exists == null || exists.intValue() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }

        int updated = em.createNativeQuery(
                        "UPDATE dbo.lesson_progress SET progress_percent = 100, completed_at = COALESCE(completed_at, SYSUTCDATETIME()), updated_at = SYSUTCDATETIME() " +
                                "WHERE user_id = :uid AND lesson_id = :lid")
                .setParameter("uid", userId)
                .setParameter("lid", lessonId)
                .executeUpdate();
        if (updated == 0) {
            em.createNativeQuery(
                            "INSERT INTO dbo.lesson_progress(user_id, lesson_id, progress_percent, completed_at, updated_at) VALUES (:uid, :lid, 100, SYSUTCDATETIME(), SYSUTCDATETIME())")
                    .setParameter("uid", userId)
                    .setParameter("lid", lessonId)
                    .executeUpdate();
        }
    }
}
