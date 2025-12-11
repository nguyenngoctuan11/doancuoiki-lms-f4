package com.example.back_end.repository;

import com.example.back_end.model.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    Optional<CourseReview> findByCourse_IdAndStudent_Id(Long courseId, Long studentId);

    List<CourseReview> findByCourse_IdAndStatusOrderByCreatedAtDesc(Long courseId, String status);

    List<CourseReview> findByStatusOrderByCreatedAtDesc(String status);

    List<CourseReview> findByInstructor_IdAndStatusOrderByCreatedAtDesc(Long instructorId, String status);

    @Query("""
            SELECT AVG(r.courseScore) AS courseAverage,
                   AVG(r.instructorScore) AS instructorAverage,
                   AVG(r.supportScore) AS supportAverage,
                   COUNT(r) AS total,
                   SUM(CASE WHEN r.wouldRecommend = true THEN 1 ELSE 0 END) AS recommendCount
            FROM CourseReview r
            WHERE r.course.id = :courseId AND r.status = 'approved'
            """)
    CourseReviewStats summaryForCourse(@Param("courseId") Long courseId);

    @Query("""
            SELECT AVG(r.courseScore) AS courseAverage,
                   AVG(r.instructorScore) AS instructorAverage,
                   AVG(r.supportScore) AS supportAverage,
                   COUNT(r) AS total,
                   SUM(CASE WHEN r.wouldRecommend = true THEN 1 ELSE 0 END) AS recommendCount
            FROM CourseReview r
            WHERE r.instructor.id = :instructorId AND r.status = 'approved'
            """)
    CourseReviewStats summaryForInstructor(@Param("instructorId") Long instructorId);

    interface CourseReviewStats {
        Double getCourseAverage();
        Double getInstructorAverage();
        Double getSupportAverage();
        Long getTotal();
        Long getRecommendCount();
    }
}
