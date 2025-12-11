package com.example.back_end.repository;

import com.example.back_end.model.LessonResource;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonResourceRepository extends JpaRepository<LessonResource, Long> {
    @EntityGraph(attributePaths = {"createdBy", "lesson", "course"})
    List<LessonResource> findByLesson_IdAndCourse_IdOrderByCreatedAtDesc(Long lessonId, Long courseId);

    @EntityGraph(attributePaths = {"createdBy", "lesson", "course"})
    List<LessonResource> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    @EntityGraph(attributePaths = {"createdBy", "lesson", "course"})
    List<LessonResource> findByCourse_IdAndVisibilityIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long courseId,
            String visibility,
            String status
    );

    @EntityGraph(attributePaths = {"createdBy", "lesson", "course"})
    List<LessonResource> findByCourse_IdAndLesson_IdAndVisibilityIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Long courseId,
            Long lessonId,
            String visibility,
            String status
    );

    @EntityGraph(attributePaths = {"createdBy", "lesson", "course"})
    @Query("SELECT lr FROM LessonResource lr " +
            "LEFT JOIN lr.course c " +
            "LEFT JOIN lr.lesson l " +
            "LEFT JOIN lr.createdBy u " +
            "WHERE (:courseId IS NULL OR c.id = :courseId) " +
            "AND (:lessonId IS NULL OR l.id = :lessonId) " +
            "AND (:status IS NULL OR LOWER(lr.status) = LOWER(:status)) " +
            "AND (:visibility IS NULL OR LOWER(lr.visibility) = LOWER(:visibility)) " +
            "AND (:keyword IS NULL OR LOWER(lr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            " OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY lr.createdAt DESC")
    List<LessonResource> searchResources(@Param("courseId") Long courseId,
                                         @Param("lessonId") Long lessonId,
                                         @Param("status") String status,
                                         @Param("visibility") String visibility,
                                         @Param("keyword") String keyword);
}
