package com.example.back_end.repository;

import com.example.back_end.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findTopByEmailIgnoreCaseAndPurposeAndCodeAndConsumedAtIsNullAndExpiresAtAfterOrderByIdDesc(
            String email, String purpose, String code, LocalDateTime now);
}

