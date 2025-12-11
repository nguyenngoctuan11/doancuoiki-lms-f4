package com.example.back_end.repository;

import com.example.back_end.model.Course;
import com.example.back_end.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import com.example.back_end.repository.projection.CourseCardProjection;
import com.example.back_end.repository.projection.CourseCardSqlProjection;
import com.example.back_end.repository.projection.ManagerPendingCourseProjection;
import com.example.back_end.repository.projection.TeacherCourseProjection;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findBySlug(String slug);

    @EntityGraph(attributePaths = "createdBy")
    List<Course> findAllByOrderByIdDesc(Pageable pageable);
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);
    List<Course> findAllByCreatedBy(User createdBy);

    @Query(value = "\n" +
            "SELECT\n" +
            "  c.id, c.title, c.slug, c.level, c.status, u.full_name AS teacherName,\n" +
            "  c.price, c.is_free AS isFree, c.thumbnail_url AS thumbnailUrl,\n" +
            "  (SELECT COUNT(*)\n" +
            "     FROM dbo.lessons l JOIN dbo.modules m ON l.module_id = m.id\n" +
            "    WHERE m.course_id = c.id) AS lessonsCount,\n" +
            "  (SELECT TOP 1 a.url\n" +
            "     FROM dbo.lesson_assets a\n" +
            "     JOIN dbo.lessons l ON a.lesson_id = l.id\n" +
            "     JOIN dbo.modules m ON l.module_id = m.id\n" +
            "    WHERE m.course_id = c.id AND a.kind = N'video'\n" +
            "    ORDER BY a.id) AS previewVideoUrl\n" +
            "FROM dbo.courses c\n" +
            "JOIN dbo.users u ON u.id = c.created_by\n" +
            "WHERE (:status IS NULL OR c.status = :status)\n" +
            "  AND (:keyword IS NULL OR c.title LIKE N'%' + :keyword + '%' OR c.slug LIKE N'%' + :keyword + '%')\n" +
            "ORDER BY c.created_at DESC\n" +
            "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY\n", nativeQuery = true)
    List<CourseCardProjection> findCourseCards(@Param("status") String status,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit,
                                               @Param("keyword") String keyword);

    // Phiên bản trả tên cột đúng T-SQL (snake_case)
    @Query(value = "\n" +
            "SELECT\n" +
            "  c.id, c.title, c.slug, c.level, c.status, u.full_name AS teacher_name,\n" +
            "  c.price, c.is_free, c.thumbnail_url,\n" +
            "  (SELECT COUNT(*)\n" +
            "     FROM dbo.lessons l JOIN dbo.modules m ON l.module_id = m.id\n" +
            "    WHERE m.course_id = c.id) AS lessons_count,\n" +
            "  (SELECT TOP 1 a.url\n" +
            "     FROM dbo.lesson_assets a\n" +
            "     JOIN dbo.lessons l ON a.lesson_id = l.id\n" +
            "     JOIN dbo.modules m ON l.module_id = m.id\n" +
            "    WHERE m.course_id = c.id AND a.kind = N'video'\n" +
            "    ORDER BY a.id) AS preview_video_url\n" +
            "FROM dbo.courses c\n" +
            "JOIN dbo.users u ON u.id = c.created_by\n" +
            "WHERE (:status IS NULL OR c.status = :status)\n" +
            "  AND (:keyword IS NULL OR c.title LIKE N'%' + :keyword + '%' OR c.slug LIKE N'%' + :keyword + '%')\n" +
            "ORDER BY c.created_at DESC\n" +
            "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY\n", nativeQuery = true)
    List<CourseCardSqlProjection> findCourseCardsSql(@Param("status") String status,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit,
                                                     @Param("keyword") String keyword);

    @Query(value = "\n" +
            "SELECT c.id, c.title, c.slug, c.level, c.status, c.approval_status AS approvalStatus,\n" +
            "       u.email AS createdByEmail, u.full_name AS createdByName, c.created_at AS createdAt\n" +
            "FROM dbo.courses c\n" +
            "JOIN dbo.users u ON u.id = c.created_by\n" +
            "WHERE c.approval_status = N'pending'\n" +
            "ORDER BY c.created_at DESC\n", nativeQuery = true)
    List<ManagerPendingCourseProjection> findPendingForManager();

    @Query(value = "\n" +
            "SELECT c.id, c.title, c.slug, c.status, c.approval_status AS approvalStatus, c.created_at AS createdAt, c.updated_at AS updatedAt\n" +
            "FROM dbo.courses c\n" +
            "JOIN dbo.users u ON u.id = c.created_by\n" +
            "WHERE u.email = :email\n" +
            "ORDER BY c.created_at DESC\n", nativeQuery = true)
    List<TeacherCourseProjection> findCoursesByCreatorEmail(@Param("email") String email);

    @Query(value = "\n" +
            "SELECT DISTINCT c.id, c.title, c.slug, c.status, c.approval_status AS approvalStatus,\n" +
            "       c.created_at AS createdAt, c.updated_at AS updatedAt\n" +
            "FROM dbo.courses c\n" +
            "LEFT JOIN dbo.course_instructors ci ON ci.course_id = c.id\n" +
            "WHERE (c.created_by = :uid OR ci.user_id = :uid)\n" +
            "  AND (:status IS NULL OR c.status = :status)\n" +
            "ORDER BY c.created_at DESC\n", nativeQuery = true)
    List<TeacherCourseProjection> findCoursesByInstructor(@Param("uid") Long userId, @Param("status") String status);

    @Query(value = "\n" +
            "SELECT c.id, c.title, c.slug, c.status, c.approval_status AS approvalStatus,\n" +
            "       c.created_at AS createdAt, c.updated_at AS updatedAt\n" +
            "FROM dbo.courses c\n" +
            "WHERE (:status IS NULL OR c.status = :status)\n" +
            "ORDER BY c.created_at DESC\n", nativeQuery = true)
    List<TeacherCourseProjection> findAllCoursesForManager(@Param("status") String status);
}
