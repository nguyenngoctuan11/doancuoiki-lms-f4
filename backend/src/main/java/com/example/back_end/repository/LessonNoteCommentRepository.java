package com.example.back_end.repository;

import com.example.back_end.model.LessonNoteComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonNoteCommentRepository extends JpaRepository<LessonNoteComment, Long> {
}
