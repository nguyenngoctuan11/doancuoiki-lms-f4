package com.example.back_end.repository;

import com.example.back_end.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByModule_IdOrderBySortOrderAscIdAsc(Long moduleId);
}
