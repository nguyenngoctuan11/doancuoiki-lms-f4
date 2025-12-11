package com.example.back_end.repository;
import com.example.back_end.model.LessonNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonNoteRepository extends JpaRepository<LessonNote, Long> {
    @EntityGraph(attributePaths = {"student", "comments", "comments.user", "course", "lesson"})
    List<LessonNote> findByLesson_IdAndCourse_IdOrderByCreatedAtAsc(Long lessonId, Long courseId);

    @EntityGraph(attributePaths = {"student", "comments", "comments.user", "course", "lesson"})
    Optional<LessonNote> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"student", "comments", "comments.user", "course", "lesson"})
    List<LessonNote> findByIdIn(List<Long> ids);
}