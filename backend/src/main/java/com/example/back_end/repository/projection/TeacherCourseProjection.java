package com.example.back_end.repository.projection;

import java.time.LocalDateTime;

public interface TeacherCourseProjection {
    Long getId();
    String getTitle();
    String getSlug();
    String getStatus();
    String getApprovalStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}

