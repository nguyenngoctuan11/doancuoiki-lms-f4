package com.example.back_end.repository.projection;

import java.math.BigDecimal;

public interface CourseCardProjection {
    Long getId();
    String getTitle();
    String getSlug();
    String getLevel();
    String getStatus();
    String getTeacherName();
    BigDecimal getPrice();
    Boolean getIsFree();
    String getThumbnailUrl();
    Integer getLessonsCount();
    String getPreviewVideoUrl();
}

