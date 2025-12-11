package com.example.back_end.repository;

import com.example.back_end.model.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<CourseModule, Long> {
    List<CourseModule> findByCourse_IdOrderBySortOrderAscIdAsc(Long courseId);
}
