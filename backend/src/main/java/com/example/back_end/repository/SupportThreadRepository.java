package com.example.back_end.repository;

import com.example.back_end.model.SupportThread;
import com.example.back_end.model.enums.SupportThreadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SupportThreadRepository extends JpaRepository<SupportThread, Long> {
    Page<SupportThread> findByStudent_Id(Long studentId, Pageable pageable);
    Optional<SupportThread> findByIdAndStudent_Id(Long id, Long studentId);
    Optional<SupportThread> findByIdAndManager_Id(Long id, Long managerId);

    @Query("""
        SELECT st FROM SupportThread st
        LEFT JOIN st.student stu
        LEFT JOIN st.course c
        WHERE (:status IS NULL OR st.status = :status)
          AND (:courseId IS NULL OR c.id = :courseId)
          AND (:fromDate IS NULL OR st.createdAt >= :fromDate)
          AND (:toDate IS NULL OR st.createdAt <= :toDate)
          AND (
                :studentKeyword IS NULL
                OR LOWER(stu.fullName) LIKE LOWER(CONCAT('%', :studentKeyword, '%'))
                OR LOWER(stu.email) LIKE LOWER(CONCAT('%', :studentKeyword, '%'))
          )
          AND (:mineOnly = false OR (st.manager.id IS NOT NULL AND st.manager.id = :managerId))
        ORDER BY st.createdAt DESC
    """)
    Page<SupportThread> searchForManager(
            @Param("status") SupportThreadStatus status,
            @Param("courseId") Long courseId,
            @Param("studentKeyword") String studentKeyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("mineOnly") boolean mineOnly,
            @Param("managerId") Long managerId,
            Pageable pageable
    );
}
