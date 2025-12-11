package com.example.back_end.repository.projection;

import java.time.LocalDateTime;

public interface ManagerPendingCourseProjection {
    Long getId();
    String getTitle();
    String getSlug();
    String getStatus();
    String getApprovalStatus();
    String getLevel();
    String getCreatedByEmail();
    String getCreatedByName();
    LocalDateTime getCreatedAt();
}

