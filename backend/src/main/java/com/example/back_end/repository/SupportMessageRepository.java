package com.example.back_end.repository;

import com.example.back_end.model.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByThread_IdOrderByCreatedAtAsc(Long threadId);
}
