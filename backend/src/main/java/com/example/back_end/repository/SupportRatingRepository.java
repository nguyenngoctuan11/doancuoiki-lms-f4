package com.example.back_end.repository;

import com.example.back_end.model.SupportRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupportRatingRepository extends JpaRepository<SupportRating, Long> {
    Optional<SupportRating> findByThread_Id(Long threadId);
    boolean existsByThread_Id(Long threadId);
}
